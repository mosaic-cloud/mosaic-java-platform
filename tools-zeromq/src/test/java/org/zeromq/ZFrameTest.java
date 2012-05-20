
package org.zeromq;


import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ.Socket;


/**
 * Tests ZFrame class
 * @author Richard Smith
 *
 */
public class ZFrameTest
{
	@Test
	public void testCopyingAndDuplicating ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zframe.test");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zframe.test");
		final ZFrame f = new ZFrame ("Hello".getBytes ());
		final ZFrame copy = f.duplicate ();
		Assert.assertTrue (copy.hasSameData (f));
		f.destroy ();
		Assert.assertFalse (copy.hasSameData (f));
		Assert.assertEquals (5, copy.size ());
		copy.destroy ();
		Assert.assertFalse (copy.hasSameData (f));
		ctx.destroy ();
	}
	
	@Test
	public void testReceiving ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zframe.test");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zframe.test");
		// Send same frame five times
		ZFrame f = new ZFrame ("Hello".getBytes ());
		for (int i = 0; i < 5; i++) {
			f.sendAndKeep (output, ZMQ.SNDMORE);
		}
		// Send END frame
		f = new ZFrame ("NOT".getBytes ());
		f.reset ("END".getBytes ());
		Assert.assertEquals ("454E44", f.strhex ());
		f.sendAndDestroy (output);
		// Read and count until we receive END
		int frame_nbr = 0;
		while (true) {
			f = ZFrame.recvFrame (input);
			frame_nbr++;
			if (f.streq ("END")) {
				f.destroy ();
				break;
			}
		}
		Assert.assertEquals (6, frame_nbr);
		f = ZFrame.recvFrame (input, ZMQ.DONTWAIT);
		Assert.assertTrue (!f.hasData ());
		ctx.destroy ();
	}
	
	@Test
	public void testSending ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zframe.test");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zframe.test");
		// Send five different frames, test ZFRAME_MORE
		for (int i = 0; i < 5; i++) {
			final ZFrame f = new ZFrame ("Hello".getBytes ());
			f.sendAndDestroy (output, ZMQ.SNDMORE);
			Assert.assertEquals (0, f.size ());
		}
		// Send same frame five times
		final ZFrame f = new ZFrame ("Hello".getBytes ());
		for (int i = 0; i < 5; i++) {
			f.sendAndKeep (output, ZMQ.SNDMORE);
		}
		Assert.assertEquals (5, f.size ());
		ctx.destroy ();
	}
	
	@Test
	public void testStringFrames ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zframe.test");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zframe.test");
		final ZFrame f1 = new ZFrame ("Hello");
		Assert.assertEquals (5, f1.getData ().length);
		f1.sendAndKeep (output);
		final ZFrame f2 = ZFrame.recvFrame (input);
		Assert.assertTrue (f2.hasData ());
		Assert.assertEquals (5, f2.getData ().length);
		Assert.assertTrue (f2.streq ("Hello"));
		Assert.assertEquals (f2.toString (), "Hello");
		Assert.assertTrue (f2.hasSameData (f1));
		ctx.destroy ();
	}
	
	@Test
	public void testZFrameCreation ()
	{
		ZFrame f = new ZFrame ("Hello".getBytes ());
		Assert.assertTrue (f != null);
		Assert.assertTrue (f.hasData ());
		Assert.assertEquals (5, f.size ());
		f = new ZFrame ();
		Assert.assertFalse (f.hasData ());
		Assert.assertEquals (0, f.size ());
	}
	
	@Test
	public void testZFrameEquals ()
	{
		final ZFrame f = new ZFrame ("Hello".getBytes ());
		final ZFrame clone = f.duplicate ();
		Assert.assertEquals (f, clone);
	}
}
