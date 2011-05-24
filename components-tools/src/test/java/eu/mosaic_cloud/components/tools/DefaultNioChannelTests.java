
package eu.mosaic_cloud.components.tools;


import java.nio.channels.Pipe;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.components.core.ChannelMessage;
import junit.framework.Assert;
import net.minidev.json.JSONObject;

import org.junit.Test;


public class DefaultNioChannelTests
{
	@Test
	public final void test ()
			throws Exception
	{
		final Pipe pipe = Pipe.open ();
		final DefaultNioChannel channel = new DefaultNioChannel (pipe.source (), pipe.sink (), DefaultJsonSimpleMessageCoder.defaultInstance, Executors.newCachedThreadPool ());
		final QueueingChannelCallbacks callbacks = new QueueingChannelCallbacks (channel);
		channel.open ();
		final int tries = 16;
		for (int index = 0; index < tries; index++)
			this.test (callbacks, RandomMessageGenerator.defaultInstance.generate ());
	}
	
	private final void test (final QueueingChannelCallbacks queue, final ChannelMessage outboundMessage)
			throws Exception
	{
		queue.add (outboundMessage);
		final ChannelMessage inboundMessage = queue.poll (1000, TimeUnit.MILLISECONDS);
		Assert.assertNotNull (inboundMessage);
		outboundMessage.data.position (0);
		outboundMessage.data.limit (outboundMessage.data.capacity ());
		((JSONObject) outboundMessage.metaData).remove ("__type__");
		Assert.assertEquals (outboundMessage, inboundMessage);
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Assert.assertTrue (arguments.length == 0);
		final DefaultNioChannelTests tests = new DefaultNioChannelTests ();
		tests.test ();
	}
}
