package mosaic.cloudlet.resources.kvstore;

/**
 * Interface for memcached-based key-value storage accessor callbacks. This
 * interface should be implemented directly or indirectly by cloudlets wishing
 * to use a memcached-based key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IMemcachedAccessorCallback<S> extends
		IKeyValueAccessorCallback<S> {
	/**
	 * Called when the add operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void addSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the add operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void addFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the append operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void appendSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the append operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void appendFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the prepend operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void prependSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the prepend operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void prependFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the replace operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void replaceSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the replace operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void replaceFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the getBulk operation completed successfully. The result of
	 * the get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the getBulk operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the cas operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void casSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the cas operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void casFailed(S state, KeyValueCallbackArguments<S> arguments);
}
