
package eu.mosaic_cloud.components.core;


public interface Channel
{
	public abstract void close ();
	
	public abstract ChannelCallbacks getCallbacks ();
	
	public abstract void send (final ChannelMessage message);
	
	public abstract void setCallbacks (final ChannelCallbacks callbacks);
}
