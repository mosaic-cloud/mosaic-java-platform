
package eu.mosaic_cloud.interoperability.core;


public interface SessionCallbacks
{
	public abstract void created (final Session session);
	
	public abstract void destroyed (final Session session);
	
	public abstract void failed (final Session session);
	
	public abstract void received (final Session session, final Message message);
}
