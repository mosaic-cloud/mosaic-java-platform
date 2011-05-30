
package eu.mosaic_cloud.components.examples.abacus;


import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallReference;
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


public class AbacusTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final int tries = 16;
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		reactor.initialize ();
		final Pipe pipe1 = Pipe.open ();
		final Pipe pipe2 = Pipe.open ();
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final BasicChannel serverChannel = BasicChannel.create (pipe1.source (), pipe2.sink (), coder, reactor, null);
		final BasicChannel clientChannel = BasicChannel.create (pipe2.source (), pipe1.sink (), coder, reactor, null);
		final BasicComponent clientComponent = BasicComponent.create (clientChannel, reactor, null);
		final BasicComponent serverComponent = BasicComponent.create (serverChannel, reactor, null);
		serverChannel.initialize ();
		clientChannel.initialize ();
		serverComponent.initialize ();
		clientComponent.initialize ();
		final AbacusComponentCallbacks serverCallbacks = new AbacusComponentCallbacks ();
		serverComponent.assign (serverCallbacks);
		final LinkedBlockingQueue<ComponentMessage> clientQueue = new LinkedBlockingQueue<ComponentMessage> ();
		final QueueingComponentCallbacks clientCallbacks = new QueueingComponentCallbacks (clientComponent, clientQueue);
		clientCallbacks.initialize ();
		for (int index = 0; index < tries; index++) {
			final int operandA = (int) (Math.random () * 10);
			final int operandB = (int) (Math.random () * 10);
			final Map<String, Object> outboundRequestMetaData = new HashMap<String, Object> ();
			outboundRequestMetaData.put ("operator", "+");
			outboundRequestMetaData.put ("operands", Arrays.asList (Integer.valueOf (operandA), Integer.valueOf (operandB)));
			final ComponentCallRequest outboundRequest = ComponentCallRequest.create (outboundRequestMetaData, ByteBuffer.allocate (0), ComponentCallReference.create ());
			clientComponent.call (peer, outboundRequest);
			final ComponentCallReply reply = (ComponentCallReply) clientQueue.poll (1000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (reply);
			final Object outcome = reply.metaData.get ("outcome");
			Assert.assertNotNull (outcome);
			Assert.assertTrue (outcome instanceof Number);
			Assert.assertEquals (outboundRequest.reference, reply.reference);
			Assert.assertTrue ((operandA + operandB) == ((Number) outcome).doubleValue ());
		}
		serverComponent.terminate ();
		clientComponent.terminate ();
		serverChannel.terminate ();
		clientChannel.terminate ();
		reactor.terminate ();
	}
}
