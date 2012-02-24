/*
 * #%L
 * mosaic-interoperability-zeromq
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

package eu.mosaic_cloud.interoperability.implementations.zeromq.tests;


import java.nio.ByteBuffer;
import java.util.UUID;

import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannelPacket;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannelSocket;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

import org.junit.Assert;
import org.junit.Test;


public final class ZeroMqChannelTest
{
	@Test
	public final void test ()
	{
		BasicThreadingSecurityManager.initialize ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions.catcher);
		Assert.assertTrue (threading.initialize (ZeroMqChannelTest.defaultPollTimeout));
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ZeroMqChannelSocket server = ZeroMqChannelSocket.create (serverIdentifier, null, threading, exceptions);
		final ZeroMqChannelSocket client = ZeroMqChannelSocket.create (clientIdentifier, null, threading, exceptions);
		server.accept (ZeroMqChannelTest.defaultServerEndpoint);
		client.connect (ZeroMqChannelTest.defaultServerEndpoint);
		for (int index = 0; index < ZeroMqChannelTest.defaultTries; index++) {
			final ByteBuffer header = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
			final ByteBuffer payload = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
			final ZeroMqChannelPacket packet1 = ZeroMqChannelPacket.create (serverIdentifier, header, payload);
			client.enqueue (packet1, ZeroMqChannelTest.defaultPollTimeout);
			final ZeroMqChannelPacket packet2 = server.dequeue (ZeroMqChannelTest.defaultPollTimeout);
			Assert.assertNotNull (packet2);
			server.enqueue (packet2, ZeroMqChannelTest.defaultPollTimeout);
			final ZeroMqChannelPacket packet3 = client.dequeue (ZeroMqChannelTest.defaultPollTimeout);
			Assert.assertNotNull (packet3);
			packet1.header.flip ();
			packet1.payload.flip ();
			Assert.assertEquals (packet1.header, packet3.header);
			Assert.assertEquals (packet1.payload, packet3.payload);
		}
		Assert.assertTrue (server.terminate (ZeroMqChannelTest.defaultPollTimeout));
		Assert.assertTrue (client.terminate (ZeroMqChannelTest.defaultPollTimeout));
		Assert.assertTrue (threading.destroy (ZeroMqChannelTest.defaultPollTimeout));
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	public static final long defaultPollTimeout = 1000;
	public static final String defaultServerEndpoint = "tcp://127.0.0.1:31027";
	public static final int defaultTries = 16;
}
