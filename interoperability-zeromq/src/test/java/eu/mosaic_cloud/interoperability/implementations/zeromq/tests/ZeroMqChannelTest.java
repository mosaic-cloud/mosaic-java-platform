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

import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannelSocket;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.Assert;
import org.junit.Test;


public final class ZeroMqChannelTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		BasicThreadingSecurityManager.initialize ();
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions.catcher);
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ByteBuffer header = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		final ByteBuffer payload = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		final ZeroMqChannelSocket server = new ZeroMqChannelSocket (serverIdentifier, null, threading, exceptions);
		server.accept (ZeroMqChannelTest.serverEndpoint);
		final ZeroMqChannelSocket client = new ZeroMqChannelSocket (clientIdentifier, null, threading, exceptions);
		client.connect (ZeroMqChannelTest.serverEndpoint);
		Threading.sleep (ZeroMqChannelTest.pollTimeout);
		final ZeroMqChannelSocket.Packet packet1 = new ZeroMqChannelSocket.Packet (serverIdentifier, header, payload);
		client.enqueue (packet1, ZeroMqChannelTest.pollTimeout);
		final ZeroMqChannelSocket.Packet packet2 = server.dequeue (ZeroMqChannelTest.pollTimeout);
		server.enqueue (packet2, ZeroMqChannelTest.pollTimeout);
		final ZeroMqChannelSocket.Packet packet3 = client.dequeue (ZeroMqChannelTest.pollTimeout);
		packet1.header.flip ();
		packet1.payload.flip ();
		Assert.assertEquals (packet1.header, packet3.header);
		Assert.assertEquals (packet1.payload, packet3.payload);
		server.terminate ();
		client.terminate ();
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	private static final long pollTimeout = 1000;
	private static final String serverEndpoint = "tcp://127.0.0.1:31027";
}
