package mosaic.cloudlet.resources;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICallback;

/**
 * Basic interface for resource accessor callback classes.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IResourceAccessorCallback<S> extends ICallback {
	/**
	 * Called when resource accessor initialization succeeded.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void initializeSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when resource accessor initialization failed.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void initializeFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Called when resource accessor destruction succeeded.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void destroySucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when resource accessor destruction failed.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void destroyFailed(S state, CallbackArguments<S> arguments);
}
