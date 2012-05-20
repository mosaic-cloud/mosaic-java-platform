
package org.zeromq;


import org.junit.Assert;
import org.junit.Test;


/**
 * @author Cliff Evans
 */
public class ZMQTest
{
	/**
	 * Test method for {@link org.zeromq.ZMQ#getVersionString()}.
	 */
	@Test
	public void testGetVersion ()
	{
		Assert.assertEquals (ZMQ.getMajorVersion () + "." + ZMQ.getMinorVersion () + "." + ZMQ.getPatchVersion (), ZMQ.getVersionString ());
	}
	
	/**
	 * Test method for {@link org.zeromq.ZMQ#makeVersion(int, int, int)}.
	 */
	@Test
	public void testMakeVersion ()
	{
		Assert.assertEquals (ZMQ.getFullVersion (), ZMQ.makeVersion (ZMQ.getMajorVersion (), ZMQ.getMinorVersion (), ZMQ.getPatchVersion ()));
	}
	
	@Test
	public void testReqRep ()
	{
		final ZMQ.Context context = ZMQ.context (1);
		final ZMQ.Socket in = context.socket (ZMQ.REQ);
		in.bind ("inproc://reqrep");
		final ZMQ.Socket out = context.socket (ZMQ.REP);
		out.connect ("inproc://reqrep");
		for (int i = 0; i < 10; i++) {
			final byte[] req = ("request" + i).getBytes ();
			final byte[] rep = ("reply" + i).getBytes ();
			Assert.assertTrue (in.send (req, 0));
			final byte[] reqTmp = out.recv (0);
			Assert.assertArrayEquals (req, reqTmp);
			Assert.assertTrue (out.send (rep, 0));
			final byte[] repTmp = in.recv (0);
			Assert.assertArrayEquals (rep, repTmp);
		}
	}
	
	@Test
	public void testXPUBSUB ()
	{
		if (ZMQ.getFullVersion () < ZMQ.make_version (3, 0, 0)) {
			// Can only test XPUB on ZMQ >= of 3.0
			return;
		}
		final ZMQ.Context context = ZMQ.context (1);
		final ZMQ.Socket pub = context.socket (ZMQ.XPUB);
		pub.bind ("inproc://xpub");
		final ZMQ.Socket sub = context.socket (ZMQ.SUB);
		sub.connect ("inproc://xpub");
		final ZMQ.Socket xsub = context.socket (ZMQ.XSUB);
		xsub.connect ("inproc://xpub");
		sub.subscribe ("".getBytes ());
		byte[] subcr = pub.recv (0);
		Assert.assertArrayEquals (new byte[] {1}, subcr);
		sub.unsubscribe ("".getBytes ());
		subcr = pub.recv (0);
		Assert.assertArrayEquals (new byte[] {0}, subcr);
		final byte[] subscription = "subs".getBytes ();
		// Append subscription
		final byte[] expected = new byte[subscription.length + 1];
		expected[0] = 1;
		System.arraycopy (subscription, 0, expected, 1, subscription.length);
		sub.subscribe (subscription);
		subcr = pub.recv (0);
		Assert.assertArrayEquals (expected, subcr);
		// Verify xsub subscription
		xsub.send (expected, 0);
		subcr = pub.recv (1);
		Assert.assertNull (subcr);
		for (int i = 0; i < 10; i++) {
			final byte[] data = ("subscrip" + i).getBytes ();
			Assert.assertTrue (pub.send (data, 0));
			// Verify SUB
			byte[] tmp = sub.recv (0);
			Assert.assertArrayEquals (data, tmp);
			// Verify XSUB
			tmp = xsub.recv (0);
			Assert.assertArrayEquals (data, tmp);
		}
	}
}
