
package eu.mosaic_cloud.components.core;


public interface ComponentCallbacks
{
	public abstract void called (final Component component, final ComponentCallRequest request);
	
	public abstract void casted (final Component component, final ComponentCast cast);
	
	public abstract void created (final Component component);
	
	public abstract void destroyed (final Component component);
	
	public abstract void failed (final Component component, final Throwable exception);
}
