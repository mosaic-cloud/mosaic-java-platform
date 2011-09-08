
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;


public interface ChannelMessageCoder
{
	public abstract ChannelMessage decode (final ByteBuffer packet)
			throws Throwable;
	
	public abstract ByteBuffer encode (final ChannelMessage message)
			throws Throwable;
}
