package mosaic.cloudlet.resources.kvstore;

import mosaic.cloudlet.resources.IResourceAccessorCallback;

/**
 * Base interface for key-value storage accessor callbacks. This interface
 * should be implemented directly or indirectly by cloudlets wishing to use a
 * key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IKeyValueAccessorCallback<S> extends
		IResourceAccessorCallback<S> {
	/**
	 * Called when the set operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void setSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the set operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void setFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the get operation completed successfully. The result of the
	 * get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the get operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the delete operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void deleteSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the delete operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void deleteFailed(S state, KeyValueCallbackArguments<S> arguments);
}
