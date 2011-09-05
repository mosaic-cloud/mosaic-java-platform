package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;

/**
 * Basic interface for AMQP accessor callbacks.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IAmqpQueueAccessorCallback<S> extends
		IResourceAccessorCallback<S> {
	/**
	 * Called when consumer or publisher registered successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void registerSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher failed to register.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void registerFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher unregistered successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void unregisterSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher failed to unregister.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void unregisterFailed(S state, CallbackArguments<S> arguments);
}
