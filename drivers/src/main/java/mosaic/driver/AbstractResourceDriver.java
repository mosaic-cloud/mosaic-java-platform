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
public abstract class AbstractResourceDriver implements IResourceDriver {
	private final List<IResult<?>> pendingResults;
	private final ExecutorService executor;
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
	public void destroy() {
		synchronized (this) {
			IResult<?> pResult;
			this.destroyed = true;
			this.executor.shutdown();
			// cancel all pending operations
			Iterator<IResult<?>> iter = this.pendingResults.iterator();
			while (iter.hasNext()) {
				pResult = iter.next();
				pResult.cancel();
				iter.remove();
			}
		}
	}

	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which return something. For the other operations
	 * see {@link AbstractResourceDriver#executeOperation(Runnable)}.
	 * 
	 * @param <T>
	 *            the operation's return type
	 * @param operation
	 *            the operation
	 */
	protected <T extends Object> void submitOperation(FutureTask<T> operation) {
		synchronized (this) {
			this.executor.submit(operation);
		}
	}

	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which do not return anything. For the other
	 * operations see {@link AbstractResourceDriver#submitOperation(FutureTask)}
	 * .
	 * 
	 * @param operation
	 *            the operation
	 */
	protected void executeOperation(Runnable operation) {
		synchronized (this) {
			this.executor.execute(operation);
		}
	}

	public int countPendingOperations() {
		synchronized (this) {
			return this.pendingResults.size();
		}
	}

	public void removePendingOperation(IResult<?> pendingOp) {
		synchronized (this) {
			this.pendingResults.remove(pendingOp);
		}
	}

	public void addPendingOperation(IResult<?> pendingOp) {
		synchronized (this) {
			this.pendingResults.add(pendingOp);
		}
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
	public void handleUnsupportedOperationError(final String opName,
			final IOperationCompletionHandler<?> handler) {
		synchronized (this) {
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
	}

	protected boolean isDestroyed() {
		synchronized (this) {
			return this.destroyed;
		}
	}

}
