package mosaic.core.ops;

/**
 * Interface for handlers to be called when the result of an event-drive
 * asynchronous operation arrives.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            the type of the result of the operation
 */
public interface IOperationCompletionHandler<T> {

	/**
	 * Handles the result of the operation. This shall be called when operation
	 * finishes successfully.
	 * 
	 * @param result
	 *            the result
	 */
	void onSuccess(T result);

	/**
	 * Handles the erroneous finish of an operation.
	 * 
	 * @param <E>
	 *            the type of the error
	 * @param error
	 *            the error
	 */
	<E extends Throwable> void onFailure(E error);
}
