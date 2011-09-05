package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.DefaultResourceAccessorCallback;

/**
 * Default AMQP resource accessor callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultAmqpAccessorCallback<S> extends
		DefaultResourceAccessorCallback<S> implements
		IAmqpQueueAccessorCallback<S> {

	@Override
	public void registerSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Register Succeeded", true,
				false);
	}

	@Override
	public void registerFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Register Failed", false, true);
	}

	@Override
	public void unregisterSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Succeeded", true,
				false);
	}

	@Override
	public void unregisterFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Failed", false,
				true);
	}

}
