
package org.zeromq;


import org.junit.Assert;
import org.junit.Test;


public class ZMQQueueTest
{
	@Test
	public void testQueue ()
			throws InterruptedException
	{
		final ZMQ.Context context = ZMQ.context (1);
		final ZMQ.Socket clients = context.socket (ZMQ.XREP);
		clients.bind ("inproc://gate_clients");
		final ZMQ.Socket workers = context.socket (ZMQ.XREQ);
		workers.bind ("inproc://gate_workers");
		final ZMQ.Socket client = context.socket (ZMQ.REQ);
		client.connect ("inproc://gate_clients");
		final ZMQ.Socket worker = context.socket (ZMQ.REP);
		worker.connect ("inproc://gate_workers");
		final Thread t = new Thread (new ZMQQueue (context, clients, workers));
		t.start ();
		for (int i = 0; i < 10; i++) {
			final byte[] req = ("request" + i).getBytes ();
			final byte[] rsp = ("response" + i).getBytes ();
			client.send (req, 0);
			// worker receives request
			final byte[] reqTmp = worker.recv (0);
			Assert.assertArrayEquals (req, reqTmp);
			// worker sends response
			worker.send (rsp, 0);
			// client receives response
			final byte[] rspTmp = client.recv (0);
			Assert.assertArrayEquals (rsp, rspTmp);
		}
		t.interrupt ();
	}
}
