
package eu.mosaic_cloud.interoperability.core;


import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;


public interface SessionCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference created (final Session session);
	
	public abstract CallbackReference destroyed (final Session session);
	
	public abstract CallbackReference failed (final Session session, final Throwable exception);
	
	public abstract CallbackReference received (final Session session, final Message message);
}
