
package eu.mosaic_cloud.components.tests;


import java.nio.channels.Pipe;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.tools.DefaultJsonMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingChannelCallbacks;
import junit.framework.Assert;

import org.junit.Test;


public final class BasicChannelTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final int tries = 16;
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		reactor.initialize ();
		final Pipe pipe = Pipe.open ();
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, null);
		final LinkedBlockingQueue<ChannelMessage> queue = new LinkedBlockingQueue<ChannelMessage> ();
		final QueueingChannelCallbacks callbacks = new QueueingChannelCallbacks (channel, queue);
		channel.initialize ();
		callbacks.initialize ();
		for (int index = 0; index < tries; index++) {
			final ChannelMessage outboundMessage = RandomMessageGenerator.defaultInstance.generateChannelMessage ();
			channel.send (outboundMessage);
			final ChannelMessage inboundMessage = queue.poll (1000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundMessage);
			Assert.assertEquals (outboundMessage.metaData, inboundMessage.metaData);
			Assert.assertEquals (outboundMessage.data, inboundMessage.data);
		}
		channel.terminate ();
		reactor.terminate ();
	}
}
