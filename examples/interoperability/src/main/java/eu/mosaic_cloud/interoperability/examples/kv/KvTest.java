/*
 * #%L
 * interoperability-examples
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

package eu.mosaic_cloud.interoperability.examples.kv;


import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

import org.junit.Assert;
import org.junit.Test;


public final class KvTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ZeroMqChannel serverChannel = new ZeroMqChannel (serverIdentifier, exceptions);
		serverChannel.register (KvSession.Server);
		serverChannel.accept (KvTest.serverEndpoint);
		final ZeroMqChannel clientChannel = new ZeroMqChannel (clientIdentifier, exceptions);
		clientChannel.register (KvSession.Client);
		clientChannel.connect (KvTest.serverEndpoint);
		final KvServer server = new KvServer ();
		server.initialize (serverChannel);
		final KvClient client_1 = new KvClient ();
		Assert.assertTrue (client_1.initialize (clientChannel, serverIdentifier).get ().booleanValue ());
		final Future<Boolean> put_a = client_1.put ("a", "1");
		final Future<Boolean> put_b = client_1.put ("b", "2");
		Assert.assertTrue (put_a.get (KvTest.pollTimeout, TimeUnit.MILLISECONDS).booleanValue ());
		Assert.assertTrue (put_b.get (KvTest.pollTimeout * 3, TimeUnit.MILLISECONDS).booleanValue ());
		final KvClient client_2 = new KvClient ();
		Assert.assertTrue (client_2.initialize (clientChannel, serverIdentifier).get ().booleanValue ());
		final Future<String> get_a = client_2.get ("a");
		final Future<String> get_b = client_2.get ("b");
		Assert.assertEquals ("1", get_a.get (KvTest.pollTimeout, TimeUnit.MILLISECONDS));
		Assert.assertEquals ("2", get_b.get (KvTest.pollTimeout, TimeUnit.MILLISECONDS));
		serverChannel.terminate (KvTest.pollTimeout);
		clientChannel.terminate (KvTest.pollTimeout);
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	private static final long pollTimeout = 1000;
	private static final String serverEndpoint = "tcp://127.0.0.1:31028";
}
