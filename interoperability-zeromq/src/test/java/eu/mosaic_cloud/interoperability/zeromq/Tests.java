
package eu.mosaic_cloud.interoperability.zeromq;


import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import junit.framework.Assert;


public final class Tests
{
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Assert.assertTrue (arguments.length == 0);
		final Tests tests = new Tests ();
		tests.testConnection ();
		tests.testChannel ();
	}
	
	@Test
	public final void testChannel ()
			throws Throwable
	{
		final String serverEndpoint = "tcp://127.0.0.1:31026";
		
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		
		final ZeroMqChannel serverChannel = new ZeroMqChannel (serverIdentifier);
		serverChannel.register (KvSession.Server);
		serverChannel.accept (serverEndpoint);
		
		final ZeroMqChannel clientChannel = new ZeroMqChannel (clientIdentifier);
		clientChannel.register (KvSession.Client);
		clientChannel.connect (serverEndpoint);
		
		final KvServer server = new KvServer ();
		server.initialize (serverChannel);
		
		final KvClient client_1 = new KvClient ();
		Assert.assertTrue (client_1.initialize (clientChannel, serverIdentifier).get ());
		
		final Future<Boolean> put_a = client_1.put ("a", "1");
		Assert.assertTrue (put_a.get (1000, TimeUnit.MILLISECONDS));
		final Future<Boolean> put_b = client_1.put ("b", "2");
		Assert.assertTrue (put_b.get (1000, TimeUnit.MILLISECONDS));
		
		final KvClient client_2 = new KvClient ();
		Assert.assertTrue (client_2.initialize (clientChannel, serverIdentifier).get ());
		final Future<String> get_a = client_2.get ("a");
		Assert.assertEquals ("1", get_a.get (1000, TimeUnit.MILLISECONDS));
		
		serverChannel.terminate ();
		clientChannel.terminate ();
	}
	
	@Test
	public final void testConnection ()
	{
		final String serverEndpoint = "tcp://127.0.0.1:31027";
		
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final byte[] header = UUID.randomUUID ().toString ().getBytes ();
		final byte[] payload = UUID.randomUUID ().toString ().getBytes ();
		
		final ZeroMqConnection server = new ZeroMqConnection (serverIdentifier, null);
		server.accept (serverEndpoint);
		final ZeroMqConnection client = new ZeroMqConnection (clientIdentifier, null);
		client.connect (serverEndpoint);
		
		final ZeroMqConnection.Packet packet1 = new ZeroMqConnection.Packet (serverIdentifier, header, payload);
		client.enqueue (packet1, 1000);
		final ZeroMqConnection.Packet packet2 = server.dequeue (1000);
		server.enqueue (packet2, 1000);
		final ZeroMqConnection.Packet packet3 = client.dequeue (1000);
		Assert.assertEquals (new String (packet1.header), new String (packet3.header));
		Assert.assertEquals (new String (packet1.payload), new String (packet3.payload));
		
		server.terminate ();
		client.terminate ();
	}
}
