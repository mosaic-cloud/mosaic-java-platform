package mosaic.driver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Base class for the resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AbstractResourceDriver implements IResourceDriver {
	private List<IResult<?>> pendingResults;
	private ExecutorService executor;
	private boolean destroyed = false;

	/**
	 * Constructs a driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 */
	protected AbstractResourceDriver(int noThreads) {
		this.pendingResults = new ArrayList<IResult<?>>();
		this.executor = Executors.newFixedThreadPool(noThreads);
	}

	@Override
	public synchronized void destroy() {
		IResult<?> pResult;

		this.destroyed = true;
		// cancel all pending operations
		Iterator<IResult<?>> it = this.pendingResults.iterator();
		while (it.hasNext()) {
			pResult = it.next();
			pResult.cancel();
			it.remove();
		}
		this.executor.shutdown();
	}

	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which return something. For the other operations
	 * see {@link AbstractResourceDriver#executeOperation(Runnable)}.
	 * 
	 * @param <T>
	 *            the operation's return type
	 * @param op
	 *            the operation
	 */
	protected synchronized <T extends Object> void submitOperation(
			FutureTask<T> op) {
		this.executor.submit(op);
	}

	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which do not return anything. For the other
	 * operations see {@link AbstractResourceDriver#submitOperation(FutureTask)}
	 * .
	 * 
	 * @param op
	 *            the operation
	 */
	protected synchronized void executeOperation(Runnable op) {
		this.executor.execute(op);
	}

	public synchronized int countPendingOperations() {
		return this.pendingResults.size();
	}

	public synchronized void removePendingOperation(IResult<?> pendingOp) {
		this.pendingResults.remove(pendingOp);
	}

	public synchronized void addPendingOperation(IResult<?> pendingOp) {
		this.pendingResults.add(pendingOp);
	}

	/**
	 * Handles unsupported operation errors. The base implementation sends an
	 * error operation to the caller.
	 * 
	 * @param opName
	 *            the name of the operation
	 * @param handler
	 *            the handler used for sending the error
	 */
	public synchronized void handleUnsupportedOperationError(
			final String opName, final IOperationCompletionHandler<?> handler) {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Exception error = new UnsupportedOperationException(
						"Operation " + opName
								+ " is not supported by this driver.");
				handler.onFailure(error);
			}
		};
		executeOperation(task);
	}

	protected synchronized boolean isDestroyed() {
		return this.destroyed;
	}
}
