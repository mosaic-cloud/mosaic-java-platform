
package eu.mosaic_cloud.components.core;


import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;


public interface ComponentCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference called (final Component component, final ComponentCallRequest request);
	
	public abstract CallbackReference callReturned (final Component component, final ComponentCallReply reply);
	
	public abstract CallbackReference casted (final Component component, final ComponentCastRequest request);
	
	public abstract CallbackReference failed (final Component component, final Throwable exception);
	
	public abstract CallbackReference initialized (final Component component);
	
	public abstract CallbackReference registerReturn (final Component component, final ComponentCallReference reference, final boolean ok);
	
	public abstract CallbackReference terminated (final Component component);
}
