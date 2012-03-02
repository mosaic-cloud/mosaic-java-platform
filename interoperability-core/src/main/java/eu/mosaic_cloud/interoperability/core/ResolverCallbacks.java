
package eu.mosaic_cloud.interoperability.core;


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;


public interface ResolverCallbacks
		extends
			Callbacks
{
	public abstract CallbackCompletion<Void> resolved (final ChannelResolver resolver, final String target, final String peer, final String endpoint);
}
