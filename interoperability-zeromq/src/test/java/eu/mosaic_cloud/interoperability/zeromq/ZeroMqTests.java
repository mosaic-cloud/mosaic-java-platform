
package eu.mosaic_cloud.interoperability.zeromq;


import java.nio.ByteBuffer;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;


public final class ZeroMqTests
{
	@Test
	public final void test ()
			throws Exception
	{
		final String serverEndpoint = "tcp://127.0.0.1:31027";
		
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ByteBuffer header = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		final ByteBuffer payload = ByteBuffer.wrap (UUID.randomUUID ().toString ().getBytes ());
		
		final ZeroMqConnection server = new ZeroMqConnection (serverIdentifier, null);
		server.accept (serverEndpoint);
		final ZeroMqConnection client = new ZeroMqConnection (clientIdentifier, null);
		client.connect (serverEndpoint);
		Thread.sleep (1000);
		
		final ZeroMqConnection.Packet packet1 = new ZeroMqConnection.Packet (serverIdentifier, header, payload);
		client.enqueue (packet1, 1000);
		final ZeroMqConnection.Packet packet2 = server.dequeue (1000);
		server.enqueue (packet2, 1000);
		final ZeroMqConnection.Packet packet3 = client.dequeue (1000);
		packet1.header.flip ();
		packet1.payload.flip ();
		Assert.assertEquals (packet1.header, packet3.header);
		Assert.assertEquals (packet1.payload, packet3.payload);
		
		server.terminate ();
		client.terminate ();
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Assert.assertTrue (arguments.length == 0);
		final ZeroMqTests tests = new ZeroMqTests ();
		tests.test ();
	}
}
