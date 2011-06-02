package mosaic.cloudlet.core;

public class OperationResultCallbackArguments<S, T> extends
		CallbackArguments<S> {
	private T result;
	private Throwable error;

	public OperationResultCallbackArguments(ICloudletController<S> cloudlet,
			T result) {
		super(cloudlet);
		this.result = result;
		this.error = null;
	}

	public OperationResultCallbackArguments(ICloudletController<S> cloudlet,
			Throwable error) {
		super(cloudlet);
		this.result = null;
		this.error = error;
	}

	public T getResult() {
		return result;
	}

	public Throwable getError() {
		return error;
	}

}
