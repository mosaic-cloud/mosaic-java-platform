
package eu.mosaic_cloud.components.tests;


import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.tools.DefaultJsonMessageCoder;
import junit.framework.Assert;

import org.junit.Test;


public final class DefaultJsonMessageCoderTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final int tries = 16;
		for (int index = 0; index < tries; index++) {
			final ChannelMessage outboundMessage = RandomMessageGenerator.defaultInstance.generateChannelMessage ();
			final ByteBuffer packet = coder.encode (outboundMessage);
			final ChannelMessage inboundMessage = coder.decode (packet);
			Assert.assertEquals (outboundMessage.metaData, inboundMessage.metaData);
			Assert.assertEquals (outboundMessage.data, inboundMessage.data);
		}
	}
}
