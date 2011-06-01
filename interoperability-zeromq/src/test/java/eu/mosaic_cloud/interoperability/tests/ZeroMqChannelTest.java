
package eu.mosaic_cloud.interoperability.tests;


import java.nio.ByteBuffer;
import java.util.UUID;

import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannelSocket;

import org.junit.Assert;
import org.junit.Test;


public final class ZeroMqChannelTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create ();
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ByteBuffer header = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		final ByteBuffer payload = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		final ZeroMqChannelSocket server = new ZeroMqChannelSocket (serverIdentifier, null, exceptions);
		server.accept (ZeroMqChannelTest.serverEndpoint);
		final ZeroMqChannelSocket client = new ZeroMqChannelSocket (clientIdentifier, null, exceptions);
		client.connect (ZeroMqChannelTest.serverEndpoint);
		Thread.sleep (ZeroMqChannelTest.pollTimeout);
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
