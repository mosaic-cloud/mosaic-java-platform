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
	protected void handleUnhandledCallback(CallbackArguments<S> arguments,
			String callbackType, boolean positive, boolean couldDestroy) {
		this.traceUnhandledCallback(arguments, callbackType);
		if (!positive && couldDestroy)
			arguments.getCloudlet().destroy();
	}

	protected void traceUnhandledCallback(CallbackArguments<S> arguments,
			String callbackType) {
		MosaicLogger.getLogger().debug(
				"unhandled cloudlet callback: `"
						+ callbackType.getClass().getName() + "`@`"
						+ callbackType + "`");
	}
}
