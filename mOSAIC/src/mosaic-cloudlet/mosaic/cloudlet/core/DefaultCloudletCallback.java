package mosaic.cloudlet.core;

/**
 * Default cloudlet callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public abstract class DefaultCloudletCallback<S> extends DefaultCallback<S>
		implements ICloudletCallback<S> {

	@Override
	public void initializeSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments,
				"Cloudlet Initialize Succeeded", true, true);

	}

	@Override
	public void initializeFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Initialize Failed",
				false, true);

	}

	@Override
	public void destroySucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Destroy Succeeded",
				true, false);

	}

	@Override
	public void destroyFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Destroy Failed",
				false, false);

	}

}
