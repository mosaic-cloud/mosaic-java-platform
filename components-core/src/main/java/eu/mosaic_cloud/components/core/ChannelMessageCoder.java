
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;


public interface ChannelMessageCoder
{
	/**
	 * Takes ownership of the byte-buffer!
	 */
	public abstract ChannelMessage decode (final ByteBuffer packet)
			throws Exception;
	
	/**
	 * Takes ownership of the message!
	 */
	public abstract ByteBuffer encode (final ChannelMessage message)
			throws Exception;
}
