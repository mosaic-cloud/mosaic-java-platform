
package org.zeromq;


import org.junit.Assert;
import org.junit.Test;


public class ZMQForwarderTest
{
	@Test
	public void testQueue ()
			throws InterruptedException
	{
		final ZMQ.Context context = ZMQ.context (1);
		final ZMQ.Socket clients = context.socket (ZMQ.PAIR);
		clients.bind ("inproc://fw_clients");
		final ZMQ.Socket client = context.socket (ZMQ.PAIR);
		client.connect ("inproc://fw_clients");
		final ZMQ.Socket workers = context.socket (ZMQ.PAIR);
		workers.bind ("inproc://fw_workers");
		final ZMQ.Socket worker = context.socket (ZMQ.PAIR);
		worker.connect ("inproc://fw_workers");
		final Thread t = new Thread (new ZMQForwarder (context, clients, workers));
		t.start ();
		for (int i = 0; i < 10; i++) {
			final byte[] req = ("request" + i).getBytes ();
			client.send (req, 0);
			// worker receives request
			final byte[] reqTmp = worker.recv (0);
			Assert.assertArrayEquals (req, reqTmp);
		}
		t.interrupt ();
	}
}
