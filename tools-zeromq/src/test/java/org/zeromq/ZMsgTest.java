
package org.zeromq;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ.Socket;


public class ZMsgTest
{
	@Test
	public void testEmptyMessage ()
	{
		final ZMsg msg = new ZMsg ();
		Assert.assertEquals (0, msg.size ());
		Assert.assertEquals (null, msg.getFirst ());
		Assert.assertEquals (null, msg.getLast ());
		Assert.assertTrue (msg.isEmpty ());
		Assert.assertEquals (null, msg.pop ());
		Assert.assertEquals (null, msg.removeFirst ());
		Assert.assertEquals (false, msg.removeFirstOccurrence (null));
		Assert.assertEquals (null, msg.removeLast ());
		msg.destroy ();
	}
	
	@Test
	public void testLoadSave ()
	{
		final ZMsg msg = new ZMsg ();
		for (int i = 0; i < 10; i++)
			msg.addString ("Frame" + i);
		try {
			// Save msg to a file
			final File f = new File ("zmsg.test");
			final DataOutputStream dos = new DataOutputStream (new FileOutputStream (f));
			Assert.assertTrue (ZMsg.save (msg, dos));
			dos.close ();
			// Read msg out of the file
			final DataInputStream dis = new DataInputStream (new FileInputStream (f));
			final ZMsg msg2 = ZMsg.load (dis);
			dis.close ();
			f.delete ();
			Assert.assertEquals (10, msg2.size ());
			Assert.assertEquals (60, msg2.contentSize ());
		} catch (final FileNotFoundException e) {
			e.printStackTrace ();
			Assert.assertTrue (false);
		} catch (final IOException e) {
			e.printStackTrace ();
			Assert.assertTrue (false);
		}
	}
	
	@Test
	public void testMessageEquals ()
	{
		final ZMsg msg = new ZMsg ();
		final ZFrame hello = new ZFrame ("Hello");
		final ZFrame world = new ZFrame ("World");
		msg.add (hello);
		msg.add (world);
		Assert.assertEquals (msg, msg.duplicate ());
		final ZMsg reverseMsg = new ZMsg ();
		msg.add (hello);
		msg.addFirst (world);
		Assert.assertFalse (msg.equals (reverseMsg));
	}
	
	@Test
	public void testMessageFrameManipulation ()
	{
		ZMsg msg = new ZMsg ();
		for (int i = 0; i < 10; i++)
			msg.addString ("Frame" + i);
		// Remove all frames apart from the first and last one
		for (int i = 0; i < 8; i++) {
			final Iterator<ZFrame> iter = msg.iterator ();
			iter.next (); // Skip first frame
			final ZFrame f = iter.next ();
			msg.remove (f);
			f.destroy ();
		}
		Assert.assertEquals (2, msg.size ());
		Assert.assertEquals (12, msg.contentSize ());
		Assert.assertTrue (msg.getFirst ().streq ("Frame0"));
		Assert.assertTrue (msg.getLast ().streq ("Frame9"));
		ZFrame f = new ZFrame ("Address");
		msg.push (f);
		Assert.assertEquals (3, msg.size ());
		Assert.assertTrue (msg.getFirst ().streq ("Address"));
		msg.addString ("Body");
		Assert.assertEquals (4, msg.size ());
		final ZFrame f0 = msg.pop ();
		Assert.assertTrue (f0.streq ("Address"));
		msg.destroy ();
		msg = new ZMsg ();
		f = new ZFrame ("Address");
		msg.wrap (f);
		Assert.assertEquals (2, msg.size ());
		msg.addString ("Body");
		Assert.assertEquals (3, msg.size ());
		f = msg.unwrap ();
		f.destroy ();
		Assert.assertEquals (1, msg.size ());
		msg.destroy ();
	}
	
	@Test
	public void testMultiPart ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zmsg.test2");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zmsg.test2");
		ZMsg msg = new ZMsg ();
		for (int i = 0; i < 10; i++)
			msg.addString ("Frame" + i);
		ZMsg copy = msg.duplicate ();
		copy.send (output);
		msg.send (output);
		copy = ZMsg.recvMsg (input);
		Assert.assertTrue (copy != null);
		Assert.assertEquals (10, copy.size ());
		Assert.assertEquals (60, copy.contentSize ());
		copy.destroy ();
		msg = ZMsg.recvMsg (input);
		Assert.assertTrue (msg != null);
		Assert.assertEquals (10, msg.size ());
		int count = 0;
		for (final ZFrame f : msg)
			Assert.assertTrue (f.streq ("Frame" + count++));
		Assert.assertEquals (60, msg.contentSize ());
		msg.destroy ();
		ctx.destroy ();
	}
	
	@Test
	public void testNewStringMessage ()
	{
		// A single string => frame
		final ZMsg msg = ZMsg.newStringMsg ("Foo");
		Assert.assertEquals (1, msg.size ());
		Assert.assertTrue (msg.getFirst ().streq ("Foo"));
		// Multiple strings => frames
		final ZMsg msg2 = ZMsg.newStringMsg ("Foo", "Bar", "Baz");
		Assert.assertEquals (3, msg2.size ());
		Assert.assertTrue (msg2.getFirst ().streq ("Foo"));
		Assert.assertTrue (msg2.getLast ().streq ("Baz"));
		// Empty message (Not very useful)
		final ZMsg msg3 = ZMsg.newStringMsg ();
		Assert.assertTrue (msg3.isEmpty ());
	}
	
	@Test
	public void testSingleFrameMessage ()
	{
		final ZContext ctx = new ZContext ();
		final Socket output = ctx.createSocket (ZMQ.PAIR);
		output.bind ("inproc://zmsg.test");
		final Socket input = ctx.createSocket (ZMQ.PAIR);
		input.connect ("inproc://zmsg.test");
		// Test send and receive of a single ZMsg
		final ZMsg msg = new ZMsg ();
		final ZFrame frame = new ZFrame ("Hello");
		msg.addFirst (frame);
		Assert.assertEquals (1, msg.size ());
		Assert.assertEquals (5, msg.contentSize ());
		msg.send (output);
		final ZMsg msg2 = ZMsg.recvMsg (input);
		Assert.assertTrue (msg2 != null);
		Assert.assertEquals (1, msg2.size ());
		Assert.assertEquals (5, msg2.contentSize ());
		msg.destroy ();
		msg2.destroy ();
		ctx.destroy ();
	}
}
