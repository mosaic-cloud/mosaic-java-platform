package mosaic.cloudlet.resources.kvstore;

import mosaic.cloudlet.resources.DefaultResourceAccessorCallback;

/**
 * Default key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultKeyValueAccessorCallback<S> extends
		DefaultResourceAccessorCallback<S> implements
		IKeyValueAccessorCallback<S> {

	@Override
	public void setSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Set Succeeded", true, false);

	}

	@Override
	public void setFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Set Failed", false, false);
	}

	@Override
	public void getSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Get Succeeded", true, false);

	}

	@Override
	public void getFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Get Failed", false, false);
	}

	@Override
	public void deleteSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Delete Succeeded", true, false);

	}

	@Override
	public void deleteFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Delete Failed", false, false);
	}

}
