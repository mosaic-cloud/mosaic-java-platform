package mosaic.driver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

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
		IResult<?> pResult;

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
	protected <T extends Object> void submitOperation(FutureTask<T> op) {
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
	protected void executeOperation(Runnable op) {
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

}
