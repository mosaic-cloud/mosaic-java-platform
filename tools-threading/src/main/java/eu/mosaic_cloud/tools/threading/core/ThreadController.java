
package eu.mosaic_cloud.tools.threading.core;


public interface ThreadController
{
	public abstract void interrupt ();
	
	public abstract boolean join ();
	
	public abstract boolean join (final long timeout);
}
