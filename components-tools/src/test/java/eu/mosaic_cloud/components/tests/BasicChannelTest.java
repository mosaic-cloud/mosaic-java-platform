
package eu.mosaic_cloud.components.tests;


import java.nio.channels.Pipe;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingChannelCallbacks;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public final class BasicChannelTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final Pipe pipe = Pipe.open ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create ();
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (exceptions);
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, exceptions);
		final QueueingChannelCallbacks callbacks = QueueingChannelCallbacks.create (channel);
		reactor.initialize ();
		channel.initialize ();
		callbacks.assign ();
		for (int index = 0; index < BasicChannelTest.tries; index++) {
			final ChannelMessage outboundMessage = RandomMessageGenerator.defaultInstance.generateChannelMessage ();
			channel.send (outboundMessage);
			final ChannelMessage inboundMessage = callbacks.queue.poll (BasicChannelTest.pollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundMessage);
			Assert.assertEquals (outboundMessage.metaData, inboundMessage.metaData);
			Assert.assertEquals (outboundMessage.data, inboundMessage.data);
		}
		pipe.sink ().close ();
		while (channel.isActive ())
			Thread.sleep (BasicChannelTest.sleepTimeout);
		Thread.sleep (BasicChannelTest.sleepTimeout);
		reactor.terminate ();
		Thread.sleep (BasicChannelTest.sleepTimeout);
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	private static final long pollTimeout = 1000;
	private static final long sleepTimeout = 100;
	private static final int tries = 16;
}
