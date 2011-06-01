
package eu.mosaic_cloud.components.examples.abacus;


import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallReference;
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


public class AbacusTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final Pipe pipe1 = Pipe.open ();
		final Pipe pipe2 = Pipe.open ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create ();
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (exceptions);
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.defaultInstance;
		final BasicChannel serverChannel = BasicChannel.create (pipe1.source (), pipe2.sink (), coder, reactor, exceptions);
		final BasicChannel clientChannel = BasicChannel.create (pipe2.source (), pipe1.sink (), coder, reactor, exceptions);
		final BasicComponent clientComponent = BasicComponent.create (clientChannel, reactor, exceptions);
		final BasicComponent serverComponent = BasicComponent.create (serverChannel, reactor, exceptions);
		final AbacusComponentCallbacks serverCallbacks = new AbacusComponentCallbacks ();
		final QueueingComponentCallbacks clientCallbacks = QueueingComponentCallbacks.create (clientComponent);
		reactor.initialize ();
		serverChannel.initialize ();
		clientChannel.initialize ();
		serverComponent.initialize ();
		clientComponent.initialize ();
		serverComponent.assign (serverCallbacks);
		clientCallbacks.assign ();
		for (int index = 0; index < AbacusTest.tries; index++) {
			final int operandA = (int) (Math.random () * 10);
			final int operandB = (int) (Math.random () * 10);
			final Map<String, Object> outboundRequestMetaData = new HashMap<String, Object> ();
			outboundRequestMetaData.put ("operator", "+");
			outboundRequestMetaData.put ("operands", Arrays.asList (Integer.valueOf (operandA), Integer.valueOf (operandB)));
			final ComponentCallRequest outboundRequest = ComponentCallRequest.create (outboundRequestMetaData, ByteBuffer.allocate (0), ComponentCallReference.create ());
			clientComponent.call (peer, outboundRequest);
			final ComponentCallReply reply = (ComponentCallReply) clientCallbacks.queue.poll (AbacusTest.pollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (reply);
			final Object outcome = reply.metaData.get ("outcome");
			Assert.assertNotNull (outcome);
			Assert.assertTrue (outcome instanceof Number);
			Assert.assertEquals (outboundRequest.reference, reply.reference);
			Assert.assertTrue ((operandA + operandB) == ((Number) outcome).doubleValue ());
		}
		pipe1.sink ().close ();
		pipe2.sink ().close ();
		while (serverComponent.isActive () || clientComponent.isActive ())
			Thread.sleep (AbacusTest.sleepTimeout);
		reactor.terminate ();
	}
	
	private static final long pollTimeout = 1000;
	private static final long sleepTimeout = 100;
	private static final int tries = 16;
}
