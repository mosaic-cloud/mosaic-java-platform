
package eu.mosaic_cloud.components.tests;


import java.nio.ByteBuffer;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;

import org.junit.Assert;
import org.junit.Test;


public final class DefaultChannelMessageCoderTest
{
	@Test
	public final void test ()
			throws Throwable
	{
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
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
