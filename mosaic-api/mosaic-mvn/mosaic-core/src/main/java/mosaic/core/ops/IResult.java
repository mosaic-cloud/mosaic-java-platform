package mosaic.core.ops;

import java.util.concurrent.ExecutionException;

/**
 * Interface for handling the result of any asynchronous operation. An object of
 * this type acts like a handle for the actual result of the operation. To
 * implement this interface, you can write a generic class extending
 * {@link GenericResult} or {@link EventDrivenResult}.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IResult<T> {
	/**
	 * Tests if the operation which will produce this result is completed and
	 * its result is ready for further processing.
	 * 
	 * @return <code>true</code> if the result is available for further
	 *         processing.
	 */
	boolean isDone();

	/**
	 * Tests if the operation which will produce this result was cancelled
	 * before it completed normally.
	 * 
	 * @return <code>true</code> if the operation was cancelled before it
	 *         completed
	 */
	boolean cancel();

	/**
	 * Waits if necessary for the asynchronous operation to complete, and then
	 * retrieves its result.
	 * 
	 * @return the computed result
	 * @throws InterruptedException
	 *             if the current thread running the operation was interrupted
	 *             while waiting
	 * @throws ExecutionException
	 *             if the operation threw an exception
	 */
	public T getResult() throws InterruptedException, ExecutionException;
}
