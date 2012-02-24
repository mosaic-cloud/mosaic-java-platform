
package eu.mosaic_cloud.tools.callbacks.core;


import java.lang.reflect.Method;


public interface CallbackFunnelHandler
		extends
			CallbackHandler
{
	public CallbackCompletion<?> executeCallback (final Callbacks Proxy, final Method method, final Object[] arguments);
}
