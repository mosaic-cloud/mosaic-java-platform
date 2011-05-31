
package eu.mosaic_cloud.components.core;


public interface Channel
{
	public abstract void assign (final ChannelCallbacks callbacks);
	
	public abstract void close (final ChannelFlow flow);
	
	public abstract void send (final ChannelMessage message);
	
	public abstract void terminate ();
}
