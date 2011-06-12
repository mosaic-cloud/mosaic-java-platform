package mosaic.cloudlet.runtime;

import java.lang.Thread.State;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import mosaic.core.log.MosaicLogger;

/**
 * A Cloudlet executor that executes operations requested in a certain cloudlet
 * instance.
 * <p>
 * Cloudlet executors provide a means of executing each cloudlet instance in a
 * single thread. Thus, a cloudlet executor will handle the execution of all
 * operations in the cloudlet as well as the ones in the response handlers
 * (callbacks).
 * 
 * 
 * @author Georgiana Macariu
 * 
 */
public class CloudletExecutor {
	/**
	 * Permission for checking shutdown
	 */
	private static final RuntimePermission shutdownPerm = new RuntimePermission(
			"modifyThread");

	/**
	 * The queue used for holding requests and handing off to the worker thread.
	 */
	private BlockingQueue<Runnable> requestQueue;

	/**
	 * The queue used for holding response handler and handing off to the worker
	 * thread.
	 */
	private BlockingQueue<Runnable> responseQueue;

	/**
	 * Lock held on updates to cloudlet request, response and runState set.
	 */
	private final ReentrantLock mainLock = new ReentrantLock();

	/**
	 * When the main worker will block waiting for a response of sent request,
	 * the backup worker should wake up and processes responses that arrive
	 * until the main worker unblocks. This is a wait condition used to awake
	 * the backup worker when the main worker gets blocked.
	 */
	private final Condition mainWorkerBlocked = mainLock.newCondition();

	/**
	 * Used for notifying the workers that there is a request or a response to a
	 * previously submitted request that is ready for processing.
	 */
	private final Condition queuesNotEmpty = mainLock.newCondition();

	/**
	 * The thread running the cloudlet. Must be volatile, to ensure visibility
	 * upon completion.
	 */
	private volatile Worker worker = null;

	/**
	 * The backup thread running the cloudlet. This will execute when the main
	 * thread is blocked waiting for an operation result. Must be volatile, to
	 * ensure visibility upon completion.
	 */
	private volatile BackupWorker backupWorker = null;

	/**
	 * runState provides the main lifecyle control, taking on values:
	 * 
	 * INITIALIZING: Initializing the cloudlet instance.
	 * 
	 * RUNNING: Processes requests.
	 * 
	 * SHUTDOWN: Don't accept new requests, but finish the current ones
	 * 
	 * STOP: Don't accept new requests, don't process queued requests and
	 * interrupt in-progress request
	 * 
	 * TERMINATED: Same as STOP, plus all threads have terminated
	 * 
	 * The numerical order among these values matters, to allow ordered
	 * comparisons. The runState monotonically increases over time, but need not
	 * hit each state. The transitions are:
	 * 
	 * 
	 * RUNNING -> SHUTDOWN<br/>
	 * On invocation of shutdown(), perhaps implicitly in finalize()
	 * 
	 * (RUNNING or SHUTDOWN) -> STOP<br/>
	 * On invocation of shutdownNow()
	 * 
	 * SHUTDOWN -> TERMINATED<br/>
	 * When the current jobs are finished and no other has been accepted for
	 * processing.
	 * 
	 * STOP -> TERMINATED<br/>
	 * When all cleanup is done.
	 */
	volatile int runState;
	/**
	 * Initializing the cloudlet instance.
	 */
	static final int INITIALIZING = 0;
	/**
	 * Processes requests.
	 */
	static final int RUNNING = 1;
	/**
	 * Don't accept new requests, but finish the current ones
	 */
	static final int SHUTDOWN = 2;
	/**
	 * Don't accept new requests, don't process queued requests and interrupt
	 * in-progress request.
	 */
	static final int STOP = 3;
	/**
	 * Same as STOP, plus all threads have terminated.
	 */
	static final int TERMINATED = 4;

	/**
	 * How many worker threads are active (>=0 && <=2).
	 */
	private volatile int runningWorkers;

	private CountDownLatch terminationLatch = new CountDownLatch(1);

	/**
	 * Creates a new CloudletExecutor.
	 */
	public CloudletExecutor() {
		super();
		this.runState = INITIALIZING;
		this.requestQueue = new LinkedBlockingQueue<Runnable>();
		this.responseQueue = new LinkedBlockingQueue<Runnable>();

		this.worker = new Worker();
		this.worker.thread = new Thread(this.worker);
		this.worker.thread.start();

		this.backupWorker = new BackupWorker();
		this.backupWorker.thread = new Thread(this.backupWorker);
		this.backupWorker.thread.start();
		runningWorkers = 2;
		this.runState = RUNNING;
		MosaicLogger.getLogger().trace("CloudletExecutor started.");
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted requests are
	 * executed, but no new requests will be accepted. Invocation has no
	 * additional effect if already shut down.
	 * 
	 * @throws SecurityException
	 *             if a security manager exists and shutting down this
	 *             {@link CloudletExecutor} may manipulate threads that the
	 *             caller is not permitted to modify because it does not hold
	 *             {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
	 *             or the security manager's <tt>checkAccess</tt> method denies
	 *             access.
	 */
	public void shutdown() {
		/*
		 * Conceptually, shutdown is just a matter of changing the runState to
		 * SHUTDOWN, and then interrupting the worker thread that might be
		 * blocked in getTask() to wake it up so it can exit. Then, if there
		 * happen not to be any tasks, we can directly terminate the executor
		 * via tryTerminate.
		 * 
		 * But this is made more delicate because we must cooperate with the
		 * security manager (if present). This requires 3 steps:
		 * 
		 * 1. Making sure caller has permission to shut down threads in general
		 * (see shutdownPerm).
		 * 
		 * 2. If (1) passes, making sure the caller is allowed to modify each of
		 * our threads. This might not be true even if first check passed, if
		 * the SecurityManager treats some threads specially. If this check
		 * passes, then we can try to set runState.
		 * 
		 * 3. If both (1) and (2) pass, dealing with inconsistent security
		 * managers that allow checkAccess but then throw a SecurityException
		 * when interrupt() is invoked. In this third case, because we have
		 * already set runState, we can only try to back out from the shutdown
		 * as cleanly as possible. Some workers may have been killed but we
		 * remain in non-shutdown state (which may entail tryTerminate from
		 * workerDone starting a new worker to maintain liveness.)
		 */

		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(shutdownPerm);

		mainLock.lock();
		try {
			if (security != null) { // Check if caller can modify our threads
				security.checkAccess(this.worker.thread);
				security.checkAccess(this.backupWorker.thread);
			}

			int state = runState;
			if (state < SHUTDOWN)
				runState = SHUTDOWN;

			try {
				this.worker.interruptIfIdle();
				if (!this.worker.isActive())
					this.backupWorker.interruptIfIdle();
			} catch (SecurityException se) { // Try to back out
				runState = state;
				throw se;
			}

			tryTerminate(); // Terminate now if queues empty
		} finally {
			mainLock.unlock();
		}
	}

	public void shutdownNow() {
		// TODO
	}

	/**
	 * Invokes {@link CloudletExecutor#shutdown()} when this executor is no
	 * longer referenced.
	 */
	protected void finalize() {
		shutdown();
	}

	/**
	 * Performs bookkeeping for an existing worker thread.
	 * 
	 * @param w
	 *            the worker
	 */
	void workerDone(Worker w) {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			if (--runningWorkers == 0)
				tryTerminate();
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * The calling thread will block waiting for the executor to shutdown.
	 * 
	 * @throws InterruptedException
	 */
	public void waitTermination() throws InterruptedException {
		this.terminationLatch.await();
	}

	/**
	 * Transitions to TERMINATED state if either (SHUTDOWN and queues empty) or
	 * (STOP and workers stopped), otherwise unless stopped, ensuring that there
	 * is at least one live thread to handle queued tasks.
	 * 
	 * This method is called from the three places in which termination can
	 * occur: in workerDone on exit of the last thread after executor has been
	 * shut down, or directly within calls to shutdown or shutdownNow, if there
	 * are no live threads.
	 */
	private void tryTerminate() {

		int state = runState;
		if (runningWorkers == 0) {
			if (state == STOP || state == SHUTDOWN) {
				runState = TERMINATED;
				this.terminationLatch.countDown();
			}
		}
	}

	/**
	 * Check whether the worker thread that fails to get a task can exit. We
	 * allow a worker thread to die if the executor is stopping, or the queue is
	 * empty, or there is at least one thread to handle possibly non-empty
	 * queue, even if core timeouts are allowed.
	 */
	private boolean workerCanExit() {
		mainLock.lock();
		boolean canExit;
		try {
			canExit = runState >= STOP
					|| (runState == SHUTDOWN && requestQueue.isEmpty() && responseQueue
							.isEmpty());
		} finally {
			mainLock.unlock();
		}
		return canExit;
		// TODO
	}

	/**
	 * Executes the given request sometime in the future.
	 * 
	 * If the request cannot be submitted for execution because this executor
	 * has been shutdown, the request is handled by the
	 * RejectedExecutionHandler.
	 * 
	 * @param request
	 *            the request to execute
	 * @throws RejectedExecutionException
	 *             if request cannot be accepted for execution
	 * @throws NullPointerException
	 *             if request is null
	 */
	public void handleRequest(Runnable request) {
		if (request == null)
			throw new NullPointerException();

		// if ((runState == RUNNING) && requestQueue.offer(request)) {
		// if (runState != RUNNING)
		// ensureQueuedRequestHandled(request);
		// } else
		// reject(request); // is shutdown

		mainLock.lock();
		try {
			if ((runState == RUNNING) && requestQueue.offer(request)) {
				queuesNotEmpty.signal();
			} else
				reject(request); // is shutdown
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * Rechecks state after queuing a request. Called from
	 * {@link CloudletExecutor#handleRequest(Runnable)} when executor state has
	 * been observed to change after queuing a request. If the request was
	 * queued concurrently with a call to {@link CloudletExecutor#shutdownNow()}
	 * , and is still present in the queue, this request must be removed and
	 * rejected to preserve {@link CloudletExecutor#shutdownNow()} guarantees.
	 * Otherwise, this method ensures that the request will be handled at some
	 * time in the future, unless executor shutdown is executed.
	 * 
	 * @param request
	 *            the request
	 */
	private void ensureQueuedRequestHandled(Runnable request) {
		mainLock.lock();
		boolean reject = false;
		try {
			int state = runState;
			if ((state != RUNNING) && requestQueue.remove(request))
				reject = true;

			if (reject)
				reject(request);
			else
				queuesNotEmpty.signal();
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * Invokes the rejected execution handler for the given request.
	 * 
	 * @throws RejectedExecutionException
	 *             alwaysF
	 */
	private void reject(Runnable request) {
		throw new RejectedExecutionException();

	}

	/**
	 * Processes the given response sometime in the future.
	 * 
	 * @param response
	 *            the response to process
	 */
	public void handleResponse(Runnable response) {
		if (response == null)
			throw new NullPointerException();
		mainLock.lock();
		try {
			if (runState < STOP && responseQueue.offer(response))
				if (worker.isBlocked())
					mainWorkerBlocked.signal();
			queuesNotEmpty.signal();
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * Gets the next task (request or response processing) for the worker thread
	 * to run. The general approach is similar to
	 * {@link CloudletExecutor#handleRequest(Runnable)} in that worker threads
	 * trying to get a task to run do so on the basis of prevailing state
	 * accessed outside of locks. This may cause them to choose the "wrong"
	 * action, such as trying to exit because no tasks appear to be available,
	 * or entering a take when the executor is in the process of being shut
	 * down. These potential problems are countered by (1) rechecking executor
	 * state (in workerCanExit) before giving up, and (2) interrupting other
	 * workers upon shutdown, so they can recheck state.
	 * 
	 * @return the task
	 */
	Runnable getTask() {
		for (;;) {
			try {
				int state = runState;
				if (state > SHUTDOWN)
					return null;
				Runnable r;
				mainLock.lock();
				if (state == SHUTDOWN) // Help drain queues
					if (!responseQueue.isEmpty())
						r = responseQueue.poll();
					else
						r = requestQueue.poll();
				else {
					while (responseQueue.isEmpty() && requestQueue.isEmpty()) {
						queuesNotEmpty.await();
					}
					if (!responseQueue.isEmpty())
						r = responseQueue.take();
					else {
						r = requestQueue.poll();
					}
				}
				if (r != null)
					return r;
				if (workerCanExit()) {
					if (runState >= SHUTDOWN) // interrupt backup thread
						interruptBackupWorker();
					return null;
				}
			} catch (InterruptedException ie) {
				// On interruption, re-check runState
			} finally {
				mainLock.unlock();
			}
		}
	}

	/**
	 * Gets the next response to process by the backup worker thread. The
	 * general approach is similar to
	 * {@link CloudletExecutor#handleRequest(Runnable)} in that worker threads
	 * trying to get a task to run do so on the basis of prevailing state
	 * accessed outside of locks. This may cause them to choose the "wrong"
	 * action, such as trying to exit because no tasks appear to be available,
	 * or entering a take when the executor is in the process of being shut
	 * down. These potential problems are countered by rechecking executor state
	 * (in workerCanExit) before giving up.
	 * 
	 * @return the task
	 */
	Runnable getBackupTask() {
		for (;;) {
			try {
				int state = runState;
				if (state > SHUTDOWN)
					return null;
				Runnable r;
				mainLock.lock();
				// wait until the main worker blocks
				while (!worker.isBlocked())
					mainWorkerBlocked.await();

				if (state >= SHUTDOWN) { // Help drain queues
					if (worker.isBlocked() && !responseQueue.isEmpty())
						r = responseQueue.poll();
					else
						r = null;
				} else {
					if (worker.isBlocked())
						r = responseQueue.take();
					else {
						r = null;
					}
				}
				return r;

			} catch (InterruptedException ie) {
				// On interruption, re-check runState
				// ie.printStackTrace();
			} finally {
				mainLock.unlock();
			}
		}
	}

	/**
	 * Wakes up the backup thread that might be waiting for responses so it can
	 * check for termination.
	 */
	private void interruptBackupWorker() {
		mainLock.lock();
		try {
			this.backupWorker.interruptIfIdle();
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * Implements the worker thread.
	 * 
	 * After completing a task, workers try to get another one, via method
	 * getTask. If they cannot (i.e., getTask returns null), they exit, calling
	 * workerDone to update pool state.
	 * 
	 * When starting to run a task, unless the executor is stopped, the worker
	 * thread ensures that it is not interrupted, and uses runLock to prevent
	 * the executor from interrupting it in the midst of execution. This shields
	 * user tasks from any interrupts that may otherwise be needed during
	 * shutdown (see method interruptIdleWorkers), unless the executor is
	 * stopping (via shutdownNow) in which case interrupts are let through to
	 * affect both tasks and workers. However, this shielding does not
	 * necessarily protect the workers from lagging interrupts from other user
	 * threads directed towards tasks that have already been completed. Thus, a
	 * worker thread may be interrupted needlessly (for example in getTask), in
	 * which case it rechecks executor state to see if it should exit.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	class Worker implements Runnable {
		/**
		 * The runLock is acquired and released surrounding each task execution.
		 * It mainly protects against interrupts that are intended to cancel the
		 * worker thread from instead interrupting the task being run.
		 */
		protected final ReentrantLock runLock = new ReentrantLock();

		/**
		 * Thread this worker is running in. Acts as a final field, but cannot
		 * be set until thread is created.
		 */
		protected Thread thread;

		Worker() {

		}

		protected boolean isActive() {
			return runLock.isLocked();
		}

		/**
		 * Interrupts thread if not running a task.
		 */
		protected void interruptIfIdle() {
			if (runLock.tryLock()) {
				try {
					if (thread != Thread.currentThread())
						thread.interrupt();
				} finally {
					runLock.unlock();
				}
			}
		}

		/**
		 * Interrupts thread even if running a task.
		 */
		protected void interruptNow() {
			thread.interrupt();
		}

		/**
		 * Runs a single task.
		 */
		private void runTask(Runnable task) {
			runLock.lock();
			try {
				/*
				 * Ensure that unless cloudlet executor is stopping, this thread
				 * does not have its interrupt set. This requires a double-check
				 * of state in case the interrupt was cleared concurrently with
				 * a shutdownNow -- if so, the interrupt is re-enabled.
				 */
				if (runState < STOP && Thread.interrupted() && runState >= STOP)
					thread.interrupt();

				task.run();
			} finally {
				runLock.unlock();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				Runnable task = null;
				while ((task = getTask()) != null) {
					runTask(task);
					task = null;
				}
			} finally {
				workerDone(this);
			}
		}

		/**
		 * Indicates if the worker is blocked waiting for the result of an
		 * asynchronous operation in a request.
		 */
		public boolean isBlocked() {
			if (isActive() && thread.getState() == State.WAITING)
				return true;
			return false;
		}
	}

	class BackupWorker extends Worker {
		BackupWorker() {
			super();
		}

		@Override
		public void run() {
			try {
				Runnable task = null;
				while ((task = getBackupTask()) != null) {
					runTask(task);
					task = null;
				}
			} finally {
				workerDone(this);
			}

		}

		/**
		 * Runs a single task.
		 */
		private void runTask(Runnable task) {
			runLock.lock();
			try {
				/*
				 * Ensure that unless cloudlet executor is stopping, this thread
				 * does not have its interrupt set. This requires a double-check
				 * of state in case the interrupt was cleared concurrently with
				 * a shutdownNow -- if so, the interrupt is re-enabled.
				 */
				if (runState < STOP && Thread.interrupted() && runState >= STOP)
					thread.interrupt();

				task.run();
			} finally {
				runLock.unlock();
			}
		}
	}
}
