
package org.zeromq;


import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ.Socket;


/**
 * Tests high-level ZContext class
 * @author richardsmith
 *
 */
public class ZContextTest
{
	@Test
	public void testAddingSockets ()
	{
		// Tests "internal" newSocket method, should not be used outside jzmq itself.
		final ZContext ctx = new ZContext ();
		try {
			final Socket s = ctx.createSocket (ZMQ.PUB);
			Assert.assertTrue (s != null);
			Assert.assertTrue (s.getType () == ZMQ.PUB);
			final Socket s1 = ctx.createSocket (ZMQ.REQ);
			Assert.assertTrue (s1 != null);
			Assert.assertEquals (2, ctx.getSockets ().size ());
		} catch (final ZMQException e) {
			System.out.println ("ZMQException:" + e.toString ());
			Assert.assertTrue (false);
		}
		ctx.destroy ();
	}
	
	@Test
	public void testConstruction ()
	{
		final ZContext ctx = new ZContext ();
		Assert.assertTrue (ctx != null);
		Assert.assertEquals (1, ctx.getIoThreads ());
		Assert.assertEquals (0, ctx.getLinger ());
		Assert.assertTrue (ctx.isMain ());
	}
	
	@Test
	public void testDestruction ()
	{
		final ZContext ctx = new ZContext ();
		ctx.destroy ();
		Assert.assertTrue (ctx.getSockets ().isEmpty ());
		// Ensure context is not destroyed if not in main thread
		final ZContext ctx1 = new ZContext ();
		ctx1.setMain (false);
		@SuppressWarnings ("unused") final Socket s = ctx1.createSocket (ZMQ.PAIR);
		ctx1.destroy ();
		Assert.assertTrue (ctx1.getSockets ().isEmpty ());
		Assert.assertTrue (ctx1.getContext () != null);
	}
	
	@Test
	public void testRemovingSockets ()
	{
		final ZContext ctx = new ZContext ();
		try {
			final Socket s = ctx.createSocket (ZMQ.PUB);
			Assert.assertTrue (s != null);
			Assert.assertEquals (1, ctx.getSockets ().size ());
			ctx.destroySocket (s);
			Assert.assertEquals (0, ctx.getSockets ().size ());
		} catch (final ZMQException e) {
			System.out.println ("ZMQException:" + e.toString ());
			Assert.assertTrue (false);
		}
		ctx.destroy ();
	}
	
	@Test
	public void testShadow ()
	{
		final ZContext ctx = new ZContext ();
		final Socket s = ctx.createSocket (ZMQ.PUB);
		Assert.assertTrue (s != null);
		Assert.assertEquals (1, ctx.getSockets ().size ());
		final ZContext shadowCtx = ZContext.shadow (ctx);
		shadowCtx.setMain (false);
		Assert.assertEquals (0, shadowCtx.getSockets ().size ());
		@SuppressWarnings ("unused") final Socket s1 = shadowCtx.createSocket (ZMQ.SUB);
		Assert.assertEquals (1, shadowCtx.getSockets ().size ());
		Assert.assertEquals (1, ctx.getSockets ().size ());
		shadowCtx.destroy ();
		ctx.destroy ();
	}
}
