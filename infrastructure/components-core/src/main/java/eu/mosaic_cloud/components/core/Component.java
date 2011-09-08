
package eu.mosaic_cloud.components.core;


public interface Component
{
	public abstract void assign (final ComponentCallbacks callbacks);
	
	public abstract void call (final ComponentIdentifier component, final ComponentCallRequest request);
	
	public abstract void cast (final ComponentIdentifier component, final ComponentCastRequest request);
	
	public abstract void register (final ComponentIdentifier group, final ComponentCallReference reference);
	
	public abstract void reply (final ComponentCallReply reply);
	
	public abstract void terminate ();
}
