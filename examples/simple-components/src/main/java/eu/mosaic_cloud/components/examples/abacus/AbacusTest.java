/*
 * #%L
 * components-examples
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.components.examples.abacus;


import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.implementations.basic.BasicComponent;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingComponentCallbacks;
import eu.mosaic_cloud.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public class AbacusTest
{
	@Test
	public final void test ()
			throws Throwable
	{
		final Pipe pipe1 = Pipe.open ();
		final Pipe pipe2 = Pipe.open ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (exceptions);
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
		final BasicChannel serverChannel = BasicChannel.create (pipe1.source (), pipe2.sink (), coder, reactor, exceptions);
		final BasicChannel clientChannel = BasicChannel.create (pipe2.source (), pipe1.sink (), coder, reactor, exceptions);
		final BasicComponent clientComponent = BasicComponent.create (clientChannel, reactor, exceptions);
		final BasicComponent serverComponent = BasicComponent.create (serverChannel, reactor, exceptions);
		final AbacusComponentCallbacks serverCallbacks = new AbacusComponentCallbacks (exceptions);
		final QueueingComponentCallbacks clientCallbacks = QueueingComponentCallbacks.create (clientComponent);
		reactor.initialize ();
		serverChannel.initialize ();
		clientChannel.initialize ();
		serverComponent.initialize ();
		clientComponent.initialize ();
		serverComponent.assign (serverCallbacks);
		clientCallbacks.assign ();
		for (int index = 0; index < AbacusTest.tries; index++) {
			final double operandA = Math.random () * 10;
			final double operandB = Math.random () * 10;
			final ComponentCallRequest request = ComponentCallRequest.create ("+", Arrays.asList (Double.valueOf (operandA), Double.valueOf (operandB)), ByteBuffer.allocate (0), ComponentCallReference.create ());
			clientComponent.call (peer, request);
			final ComponentCallReply reply = (ComponentCallReply) clientCallbacks.queue.poll (AbacusTest.pollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (reply);
			Assert.assertTrue (reply.ok);
			Assert.assertNotNull (reply.outputsOrError);
			Assert.assertEquals (request.reference, reply.reference);
			Assert.assertTrue ((operandA + operandB) == ((Number) reply.outputsOrError).doubleValue ());
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
