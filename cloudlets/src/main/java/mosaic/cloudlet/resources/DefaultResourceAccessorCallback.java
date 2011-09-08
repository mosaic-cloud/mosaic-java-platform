package mosaic.cloudlet.resources;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCallback;

/**
 * Default resource accessor callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultResourceAccessorCallback<S> extends DefaultCallback<S>
		implements IResourceAccessorCallback<S> {

	@Override
	public void initializeSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments,
				"Resource Initialize Succeeded", true, true);
	}

	@Override
	public void initializeFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Resource Initialize Failed",
				false, true);
	}

	@Override
	public void destroySucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Resource Destroy Succeeded",
				true, false);
	}

	@Override
	public void destroyFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Resource Destroy Failed",
				false, false);
	}

}
