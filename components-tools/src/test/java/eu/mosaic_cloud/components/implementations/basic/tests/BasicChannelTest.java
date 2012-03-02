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

import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelController;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.implementations.basic.BasicChannel;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.components.tools.QueueingChannelCallbacks;
import eu.mosaic_cloud.components.tools.tests.RandomMessageGenerator;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public final class BasicChannelTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final Transcript transcript = Transcript.create (this);
		BasicThreadingSecurityManager.initialize ();
		final Pipe pipe = Pipe.open ();
		final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, exceptionsQueue);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions, exceptions.catcher);
		Assert.assertTrue (threading.initialize (BasicChannelTest.defaultPollTimeout));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (threading, exceptions);
		Assert.assertTrue (reactor.initialize (BasicChannelTest.defaultPollTimeout));
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.defaultInstance;
		final BasicChannel channel = BasicChannel.create (pipe.source (), pipe.sink (), coder, reactor, threading, exceptions);
		Assert.assertTrue (channel.initialize (BasicChannelTest.defaultPollTimeout));
		final ChannelController channelController = channel.getController ();
		final ChannelCallbacks channelCallbacksProxy = reactor.createProxy (ChannelCallbacks.class);
		Assert.assertTrue (channelController.bind (channelCallbacksProxy).await (BasicChannelTest.defaultPollTimeout));
		final QueueingChannelCallbacks channelCallbacks = QueueingChannelCallbacks.create (channelController, exceptions);
		final CallbackIsolate channelCallbacksIsolate = reactor.createIsolate ();
		Assert.assertTrue (reactor.assignHandler (channelCallbacksProxy, channelCallbacks, channelCallbacksIsolate).await (BasicChannelTest.defaultPollTimeout));
		for (int index = 0; index < BasicChannelTest.defaultTries; index++) {
			final ChannelMessage outboundMessage = RandomMessageGenerator.defaultInstance.generateChannelMessage ();
			Assert.assertTrue (channelController.send (outboundMessage).await (BasicChannelTest.defaultPollTimeout));
			final ChannelMessage inboundMessage = channelCallbacks.queue.poll (BasicChannelTest.defaultPollTimeout, TimeUnit.MILLISECONDS);
			Assert.assertNotNull (inboundMessage);
			Assert.assertEquals (outboundMessage.metaData, inboundMessage.metaData);
			Assert.assertEquals (outboundMessage.data, inboundMessage.data);
		}
		pipe.sink ().close ();
		Assert.assertTrue (channel.destroy (BasicChannelTest.defaultPollTimeout));
		Assert.assertTrue (channelCallbacksIsolate.destroy ().await (BasicChannelTest.defaultPollTimeout));
		Assert.assertTrue (reactor.destroy (BasicChannelTest.defaultPollTimeout));
		Assert.assertTrue (threading.destroy (BasicChannelTest.defaultPollTimeout));
		Assert.assertNull (exceptionsQueue.queue.poll ());
	}
	
	public static final long defaultPollTimeout = 1000;
	public static final int defaultTries = 16;
}
