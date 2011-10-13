package mosaic.core.ops;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Interface for asynchronous operations.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the operation.
 */
public interface IOperation<T> {

	/**
	 * Cancels the asynchronous operation.
	 * 
	 * @return <code>true</code> if operation was cancelled
	 */
	boolean cancel();

	/**
	 * Returns <code>true</code> if this task was cancelled before it completed
	 * normally.
	 * 
	 * @return <code>true</code> if this task was cancelled before it completed
	 */
	boolean isCancelled();

	/**
	 * Returns <code>true</code> if this task completed. Completion may be due
	 * to normal termination, an exception, or cancellation -- in all of these
	 * cases, this method will return <code>true</code>.
	 * 
	 * @return <code>true</code> if this task completed
	 */
	boolean isDone();

	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its result.
	 * 
	 * @return the computed result
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 */
	T get() throws InterruptedException, ExecutionException;

	/**
	 * Waits if necessary for at most the given time for the computation to
	 * complete, and then retrieves its result, if available.
	 * 
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return the computed result
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws TimeoutException
	 *             if the wait timed out
	 */
	T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException;
}
