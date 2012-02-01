/*
 * #%L
 * mosaic-examples-interoperability
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

package eu.mosaic_cloud.examples.interoperability.kv;


import java.util.UUID;

import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.Assert;
import org.junit.Test;


public final class KvTest
{
	@Test
	public final void test ()
	{
		BasicThreadingSecurityManager.initialize ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions.catcher);
		Assert.assertTrue (threading.initialize (KvTest.defaultPollTimeout));
		final String serverIdentifier = UUID.randomUUID ().toString ();
		final String clientIdentifier = UUID.randomUUID ().toString ();
		final ZeroMqChannel serverChannel = ZeroMqChannel.create (serverIdentifier, threading, exceptions);
		serverChannel.register (KvSession.Server);
		serverChannel.accept (KvTest.defaultServerEndpoint);
		final ZeroMqChannel clientChannel = ZeroMqChannel.create (clientIdentifier, threading, exceptions);
		clientChannel.register (KvSession.Client);
		clientChannel.connect (KvTest.defaultServerEndpoint);
		final KvServer server = new KvServer (exceptions, KvTest.defaultPollTimeout);
		server.initialize (serverChannel);
		final KvClient client_1 = new KvClient ();
		Assert.assertEquals (Boolean.TRUE, Threading.awaitOrCatch (client_1.initialize (clientChannel, serverIdentifier), KvTest.defaultPollTimeout));
		Assert.assertEquals (Boolean.TRUE, Threading.awaitOrCatch (client_1.put ("a", "1"), KvTest.defaultPollTimeout));
		Assert.assertEquals (Boolean.TRUE, Threading.awaitOrCatch (client_1.put ("b", "2"), KvTest.defaultPollTimeout));
		final KvClient client_2 = new KvClient ();
		Assert.assertEquals (Boolean.TRUE, Threading.awaitOrCatch (client_2.initialize (clientChannel, serverIdentifier), KvTest.defaultPollTimeout));
		Assert.assertEquals ("1", Threading.awaitOrCatch (client_2.get ("a"), KvTest.defaultPollTimeout));
		Assert.assertEquals ("2", Threading.awaitOrCatch (client_2.get ("b"), KvTest.defaultPollTimeout));
		Assert.assertTrue (serverChannel.terminate (KvTest.defaultPollTimeout));
		Assert.assertTrue (clientChannel.terminate (KvTest.defaultPollTimeout));
		Assert.assertTrue (threading.destroy (KvTest.defaultPollTimeout));
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	public static final long defaultPollTimeout = 1000;
	public static final String defaultServerEndpoint = "tcp://127.0.0.1:31028";
}
