
package eu.mosaic_cloud.tools.threading.core;


public interface Joinable
{
	public abstract boolean await ();
	
	public abstract boolean await (final long timeout);
}
