package mosaic.cloudlet.core;

import mosaic.core.log.MosaicLogger;

/**
 * Default callback class.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultCallback<S> implements ICallback {
	/**
	 * Handles any unhandled callback.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful
	 *            termination of the operation
	 * @param couldDestroy
	 *            <code>true</code> if cloudlet can be destroyed here
	 */
	protected void handleUnhandledCallback(CallbackArguments<S> arguments,
			String callbackType, boolean positive, boolean couldDestroy) {
		this.traceUnhandledCallback(arguments, callbackType, positive);
		if (!positive && couldDestroy) {
			arguments.getCloudlet().destroy();
		}
	}

	/**
	 * Traces unhandled callbacks.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful
	 *            termination of the operation
	 */
	protected void traceUnhandledCallback(CallbackArguments<S> arguments,
			String callbackType, boolean positive) {
		MosaicLogger.getLogger().info(
				"unhandled cloudlet callback: `" + this.getClass().getName()
						+ "`@`" + callbackType + "` "
						+ (positive ? "Succeeded" : "Failed"));
	}
}
