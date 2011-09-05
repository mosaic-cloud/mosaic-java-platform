package mosaic.cloudlet.resources.kvstore;

/**
 * Default memcached key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultMemcachedAccessorCallback<S> extends
		DefaultKeyValueAccessorCallback<S> implements
		IMemcachedAccessorCallback<S> {

	@Override
	public void addSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Add Succeeded", true, false);

	}

	@Override
	public void addFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Add Failed", false, false);

	}

	@Override
	public void appendSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Append Succeeded", true, false);

	}

	@Override
	public void appendFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Append Failed", false, false);

	}

	@Override
	public void prependSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Prepend Succeeded", true,
				false);

	}

	@Override
	public void prependFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Prepend Failed", false, false);

	}

	@Override
	public void replaceSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Replace Succeeded", true,
				false);

	}

	@Override
	public void replaceFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Replace Failed", false, false);

	}

	@Override
	public void getBulkSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "GetBulk Succeeded", true,
				false);

	}

	@Override
	public void getBulkFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "GetBulk Failed", false, false);

	}

	@Override
	public void casSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cas Succeeded", true, false);

	}

	@Override
	public void casFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cas Failed", false, false);

	}

}
