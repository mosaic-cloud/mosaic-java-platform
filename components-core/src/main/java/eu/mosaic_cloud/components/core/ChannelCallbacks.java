
package eu.mosaic_cloud.components.core;


public interface ChannelCallbacks
{
	public abstract void closed (final Channel channel);
	
	public abstract void failed (final Channel channel, final Throwable exception);
	
	public abstract void opened (final Channel channel);
	
	public abstract void received (final Channel channel, final ChannelMessage message);
}
