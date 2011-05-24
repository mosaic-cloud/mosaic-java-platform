
package eu.mosaic_cloud.components.core;


public interface PeerComponent
{
	public abstract void call (final ComponentCallRequest request);
	
	public abstract void cast (final ComponentCast cast);
}
