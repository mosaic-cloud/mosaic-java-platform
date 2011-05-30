
package eu.mosaic_cloud.components.tests;


import java.nio.channels.Pipe;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentMessage;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.implementations.basic.BasicComponent;
import eu.mosaic_cloud.components.tools.DefaultJsonMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingComponentCallbacks;
import junit.framework.Assert;

import org.junit.Test;


public class BasicComponentTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final int tries = 16;
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		reactor.initialize ();
		final Pipe pipe = Pipe.open ();
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, null);
		final BasicComponent component = BasicComponent.create (channel, reactor, null);
		final LinkedBlockingQueue<ComponentMessage> queue = new LinkedBlockingQueue<ComponentMessage> ();
		final QueueingComponentCallbacks callbacks = new QueueingComponentCallbacks (component, queue);
		channel.initialize ();
		component.initialize ();
		callbacks.initialize ();
		for (int index = 0; index < tries; index++) {
			final ComponentCallRequest outboundRequest = RandomMessageGenerator.defaultInstance.generateComponentCallRequest ();
			component.call (peer, outboundRequest);
			final ComponentCallRequest inboundRequest = (ComponentCallRequest) queue.poll (1000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundRequest);
			Assert.assertEquals (outboundRequest.metaData, inboundRequest.metaData);
			Assert.assertEquals (outboundRequest.data, inboundRequest.data);
			final ComponentCallReply outboundReply = RandomMessageGenerator.defaultInstance.generateComponentCallReply (inboundRequest);
			component.reply (outboundReply);
			final ComponentCallReply inboundReply = (ComponentCallReply) queue.poll (1000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundReply);
			Assert.assertEquals (outboundReply.metaData, inboundReply.metaData);
			Assert.assertEquals (outboundReply.data, inboundReply.data);
		}
		component.terminate ();
		channel.terminate ();
		reactor.terminate ();
	}
}
