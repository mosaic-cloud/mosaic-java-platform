
package eu.mosaic_cloud.components.core;


public interface Component
{
	public abstract ComponentCallbacks getCallbacks ();
	
	public abstract PeerComponent resolve (final ComponentIdentifier identifier);
	
	public abstract void setCallbacks (final ComponentCallbacks callbacks);
	
	public abstract void terminate ();
}
