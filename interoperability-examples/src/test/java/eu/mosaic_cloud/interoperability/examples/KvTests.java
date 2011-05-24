
package eu.mosaic_cloud.interoperability.examples;


import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

import junit.framework.Assert;

import org.junit.Test;


public final class KvTests
{
	@Test
	public final void test ()
			throws Exception
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
		Assert.assertTrue (client_1.initialize (clientChannel, serverIdentifier).get ().booleanValue ());
		final Future<Boolean> put_a = client_1.put ("a", "1");
		final Future<Boolean> put_b = client_1.put ("b", "2");
		Assert.assertTrue (put_a.get (1000, TimeUnit.MILLISECONDS).booleanValue ());
		Assert.assertTrue (put_b.get (2000 + 1000, TimeUnit.MILLISECONDS).booleanValue ());
		
		final KvClient client_2 = new KvClient ();
		Assert.assertTrue (client_2.initialize (clientChannel, serverIdentifier).get ().booleanValue ());
		final Future<String> get_a = client_2.get ("a");
		final Future<String> get_b = client_2.get ("b");
		Assert.assertEquals ("1", get_a.get (1000, TimeUnit.MILLISECONDS));
		Assert.assertEquals ("2", get_b.get (1000, TimeUnit.MILLISECONDS));
		
		serverChannel.terminate (1000);
		clientChannel.terminate (1000);
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Assert.assertTrue (arguments.length == 0);
		final KvTests tests = new KvTests ();
		tests.test ();
	}
}
