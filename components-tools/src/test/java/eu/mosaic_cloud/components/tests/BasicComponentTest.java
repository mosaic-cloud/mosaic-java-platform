
package eu.mosaic_cloud.components.tests;


import java.nio.channels.Pipe;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.implementations.basic.BasicComponent;
import eu.mosaic_cloud.components.tools.DefaultJsonMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingComponentCallbacks;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public final class BasicComponentTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final Pipe pipe = Pipe.open ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create ();
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (exceptions);
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, exceptions);
		final BasicComponent component = BasicComponent.create (channel, reactor, exceptions);
		final QueueingComponentCallbacks callbacks = QueueingComponentCallbacks.create (component);
		reactor.initialize ();
		channel.initialize ();
		component.initialize ();
		callbacks.assign ();
		for (int index = 0; index < BasicComponentTest.tries; index++) {
			final ComponentCallRequest outboundRequest = RandomMessageGenerator.defaultInstance.generateComponentCallRequest ();
			component.call (peer, outboundRequest);
			final ComponentCallRequest inboundRequest = (ComponentCallRequest) callbacks.queue.poll (BasicComponentTest.pollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundRequest);
			Assert.assertEquals (outboundRequest.metaData, inboundRequest.metaData);
			Assert.assertEquals (outboundRequest.data, inboundRequest.data);
			final ComponentCallReply outboundReply = RandomMessageGenerator.defaultInstance.generateComponentCallReply (inboundRequest);
			component.reply (outboundReply);
			final ComponentCallReply inboundReply = (ComponentCallReply) callbacks.queue.poll (BasicComponentTest.pollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundReply);
			Assert.assertEquals (outboundRequest.reference, inboundReply.reference);
			Assert.assertEquals (outboundRequest.metaData, inboundReply.metaData);
			Assert.assertEquals (outboundRequest.data, inboundReply.data);
		}
		pipe.sink ().close ();
		while (component.isActive ())
			Thread.sleep (BasicComponentTest.sleepTimeout);
		Thread.sleep (BasicComponentTest.sleepTimeout);
		reactor.terminate ();
		Thread.sleep (BasicComponentTest.sleepTimeout);
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	private static final long pollTimeout = 1000;
	private static final long sleepTimeout = 100;
	private static final int tries = 16;
}
