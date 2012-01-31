/*
 * #%L
 * mosaic-components-tools
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.components.implementations.basic.tests;


import java.nio.channels.Pipe;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.implementations.basic.BasicComponent;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingComponentCallbacks;
import eu.mosaic_cloud.components.tools.tests.RandomMessageGenerator;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.Assert;
import org.junit.Test;


public final class BasicComponentTest
{
	@Test
	public final void test ()
			throws Exception
	{
		BasicThreadingSecurityManager.initialize ();
		final Pipe pipe = Pipe.open ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions.catcher);
		final ComponentIdentifier peer = ComponentIdentifier.resolve (Strings.repeat ("00", 20));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (threading, exceptions);
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, threading, exceptions);
		final BasicComponent component = BasicComponent.create (channel, reactor, exceptions);
		final QueueingComponentCallbacks callbacks = QueueingComponentCallbacks.create (component);
		Assert.assertTrue (reactor.initialize (BasicComponentTest.defaultPollTimeout));
		Assert.assertTrue (channel.initialize (BasicComponentTest.defaultPollTimeout));
		Assert.assertTrue (component.initialize (BasicComponentTest.defaultPollTimeout));
		callbacks.assign ();
		for (int index = 0; index < BasicComponentTest.defaultTries; index++) {
			final ComponentCallRequest outboundRequest = RandomMessageGenerator.defaultInstance.generateComponentCallRequest ();
			component.call (peer, outboundRequest);
			final ComponentCallRequest inboundRequest = (ComponentCallRequest) callbacks.queue.poll (BasicComponentTest.defaultPollTimeout * 10000, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundRequest);
			Assert.assertEquals (outboundRequest.operation, inboundRequest.operation);
			Assert.assertEquals (outboundRequest.inputs, inboundRequest.inputs);
			Assert.assertEquals (outboundRequest.data, inboundRequest.data);
			final ComponentCallReply outboundReply = RandomMessageGenerator.defaultInstance.generateComponentCallReply (inboundRequest);
			component.reply (outboundReply);
			final ComponentCallReply inboundReply = (ComponentCallReply) callbacks.queue.poll (BasicComponentTest.defaultPollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundReply);
			Assert.assertEquals (outboundRequest.reference, inboundReply.reference);
			Assert.assertEquals (outboundRequest.inputs, inboundReply.outputsOrError);
			Assert.assertEquals (outboundRequest.data, inboundReply.data);
		}
		pipe.sink ().close ();
		Assert.assertTrue (component.terminate (BasicComponentTest.defaultPollTimeout));
		Assert.assertTrue (channel.terminate (BasicComponentTest.defaultPollTimeout));
		Threading.sleep (BasicComponentTest.defaultPollTimeout);
		Assert.assertTrue (reactor.destroy (BasicComponentTest.defaultPollTimeout));
		Assert.assertTrue (threading.await (BasicComponentTest.defaultPollTimeout));
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	public static final long defaultPollTimeout = 1000;
	public static final int defaultTries = 16;
}
