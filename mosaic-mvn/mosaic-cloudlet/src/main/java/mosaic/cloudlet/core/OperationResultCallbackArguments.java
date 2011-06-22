package mosaic.cloudlet.core;

/**
 * Base class for clouldet callback arguments. This will hold a reference to the
 * cloudlet controller but also the result of the operation or the exception
 * thrown by the operation.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 * @param <T>
 *            the type of the result of the operation
 */
public class OperationResultCallbackArguments<S, T> extends
		CallbackArguments<S> {
	private T result;
	private Throwable error;

	/**
	 * Creates the operation callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet controller
	 * @param result
	 *            the result of the operation
	 */
	public OperationResultCallbackArguments(ICloudletController<S> cloudlet,
			T result) {
		super(cloudlet);
		this.result = result;
		this.error = null;
	}

	/**
	 * Creates the operation callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet controller
	 * @param error
	 *            the exception thrown by the operation
	 */
	public OperationResultCallbackArguments(ICloudletController<S> cloudlet,
			Throwable error) {
		super(cloudlet);
		this.result = null;
		this.error = error;
	}

	/**
	 * Returns the result of the operation.
	 * 
	 * @return the result of the operation
	 */
	public T getResult() {
		return result;
	}

	/**
	 * Returns the exception thrown by the operation if it didn't finish with
	 * success.
	 * 
	 * @return the exception thrown by the operation
	 */
	public Throwable getError() {
		return error;
	}

}
