
package eu.mosaic_cloud.components.tools.tests;


import java.nio.ByteBuffer;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.tools.DefaultJsonSimpleMessageCoder;
import junit.framework.Assert;
import net.minidev.json.JSONObject;

import org.junit.Test;


public class DefaultJsonSimpleMessageCoderTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final int tries = 16;
		for (int index = 0; index < tries; index++)
			this.test (DefaultJsonSimpleMessageCoder.defaultInstance, RandomMessageGenerator.defaultInstance.generate ());
	}
	
	private final void test (final DefaultJsonSimpleMessageCoder coder, final ChannelMessage outboundMessage)
			throws Exception
	{
		final ByteBuffer packet = coder.encode (outboundMessage);
		final ChannelMessage inboundMessage = coder.decode (packet);
		outboundMessage.data.position (0);
		outboundMessage.data.limit (outboundMessage.data.capacity ());
		((JSONObject) outboundMessage.metaData).remove ("__type__");
		Assert.assertEquals (outboundMessage, inboundMessage);
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Assert.assertTrue (arguments.length == 0);
		final DefaultJsonSimpleMessageCoderTest tests = new DefaultJsonSimpleMessageCoderTest ();
		tests.test ();
	}
}
