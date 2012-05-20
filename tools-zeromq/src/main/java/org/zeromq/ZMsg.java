
package org.zeromq;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.zeromq.ZMQ.Socket;


/**
 * The ZMsg class provides methods to send and receive multipart messages
 * across 0MQ sockets. This class provides a list-like container interface,
 * with methods to work with the overall container.  ZMsg messages are
 * composed of zero or more ZFrame objects.
 * 
 * <pre>
 * // Send a simple single-frame string message on a ZMQSocket "output" socket object
 * ZMsg.newStringMsg("Hello").send(output);
 * 
 * // Add several frames into one message
 * ZMsg msg = new ZMsg();
 * for (int i = 0;i< 10;i++) {
 *     msg.addString("Frame" + i);
 * }
 * msg.send(output);
 * 
 * // Receive message from ZMQSocket "input" socket object and iterate over frames
 * ZMsg receivedMessage = ZMsg.recvMsg(input);
 * for (ZFrame f : receivedMessage) {
 *     // Do something with frame f (of type ZFrame)
 * }
 * </pre>
 *
 * Based on <a href="http://github.com/zeromq/czmq/blob/master/src/zmsg.c">zmsg.c</a> in czmq
 *
 */
public class ZMsg
		implements
			Iterable<ZFrame>,
			Deque<ZFrame>
{
	/**
	 * Class Constructor
	 */
	public ZMsg ()
	{
		this.frames = new ArrayDeque<ZFrame> ();
	}
	
	public boolean add (final byte[] data)
	{
		return this.add (new ZFrame (data));
	}
	
	public boolean add (final String stringValue)
	{
		return this.add (new ZFrame (stringValue));
	}
	
	@Override
	public boolean add (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		return this.frames.add (e);
	}
	
	// ********* Implement Deque Interface ****************** //
	@Override
	public boolean addAll (final Collection<? extends ZFrame> arg0)
	{
		return this.frames.addAll (arg0);
	}
	
	public void addFirst (final byte[] data)
	{
		this.addFirst (new ZFrame (data));
	}
	
	public void addFirst (final String stringValue)
	{
		this.addFirst (new ZFrame (stringValue));
	}
	
	@Override
	public void addFirst (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		this.frames.addFirst (e);
	}
	
	public void addLast (final byte[] data)
	{
		this.addLast (new ZFrame (data));
	}
	
	public void addLast (final String stringValue)
	{
		this.addLast (new ZFrame (stringValue));
	}
	
	@Override
	public void addLast (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		this.frames.addLast (e);
	}
	
	/**
	 * Add a String as a new ZFrame to the end of list
	 * @param str
	 * 				String to add to list
	 */
	public void addString (final String str)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		this.frames.add (new ZFrame (str));
	}
	
	@Override
	public void clear ()
	{
		this.frames.clear ();
	}
	
	@Override
	public boolean contains (final Object o)
	{
		return this.frames.contains (o);
	}
	
	@Override
	public boolean containsAll (final Collection<?> arg0)
	{
		return this.frames.containsAll (arg0);
	}
	
	/**
	 * Return total number of bytes contained in all ZFrames in this ZMsg
	 * @return
	 */
	public long contentSize ()
	{
		long size = 0;
		for (final ZFrame f : this.frames) {
			size += f.size ();
		}
		return size;
	}
	
	@Override
	public Iterator<ZFrame> descendingIterator ()
	{
		return this.frames.descendingIterator ();
	}
	
	/**
	 * Destructor.
	 * Explicitly destroys all ZFrames contains in the ZMsg
	 */
	public void destroy ()
	{
		if (this.frames == null)
			return;
		for (final ZFrame f : this.frames) {
			f.destroy ();
		}
		this.frames.clear ();
	}
	
	// ********* Convenience Deque methods for common data types *** //
	/**
	 * Dump the message in human readable format. This should only be used
	 * for debugging and tracing, inefficient in handling large messages. 
	 **/
	public void dump (final Appendable out)
	{
		try {
			final StringWriter sw = new StringWriter ();
			final PrintWriter pw = new PrintWriter (sw);
			pw.printf ("--------------------------------------\n");
			for (final ZFrame frame : this.frames) {
				pw.printf ("[%03d] %s\n", frame.getData ().length, frame.toString ());
			}
			out.append (sw.getBuffer ());
			sw.close ();
		} catch (final IOException e) {
			throw new RuntimeException ("Message dump exception " + super.toString (), e);
		}
	}
	
	/**
	 * Creates copy of this ZMsg.
	 * Also duplicates all frame content.
	 * @return
	 * 			The duplicated ZMsg object, else null if this ZMsg contains an empty frame set
	 */
	public ZMsg duplicate ()
	{
		if (this.frames != null) {
			final ZMsg msg = new ZMsg ();
			for (final ZFrame f : this.frames)
				msg.add (f.duplicate ());
			return msg;
		} else
			return null;
	}
	
	@Override
	public ZFrame element ()
	{
		return this.frames.element ();
	}
	
	@Override
	public boolean equals (final Object o)
	{
		if (this == o)
			return true;
		if ((o == null) || (this.getClass () != o.getClass ()))
			return false;
		final ZMsg zMsg = (ZMsg) o;
		if ((this.frames == null) || (zMsg.frames == null))
			return false;
		//based on AbstractList
		final Iterator<ZFrame> e1 = this.frames.iterator ();
		final Iterator<ZFrame> e2 = zMsg.frames.iterator ();
		while (e1.hasNext () && e2.hasNext ()) {
			final ZFrame o1 = e1.next ();
			final ZFrame o2 = e2.next ();
			if (!(o1 == null ? o2 == null : o1.equals (o2)))
				return false;
		}
		return !(e1.hasNext () || e2.hasNext ());
	}
	
	// ********* Convenience Queue methods for common data types *** //
	@Override
	public ZFrame getFirst ()
	{
		try {
			return this.frames.getFirst ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public ZFrame getLast ()
	{
		try {
			return this.frames.getLast ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public int hashCode ()
	{
		if ((this.frames == null) || (this.frames.size () == 0))
			return 0;
		int result = 1;
		for (final ZFrame frame : this.frames)
			result = (31 * result) + (frame == null ? 0 : frame.hashCode ());
		return result;
	}
	
	@Override
	public boolean isEmpty ()
	{
		return this.frames.isEmpty ();
	}
	
	// ********* Implement Iterable Interface *************** //
	@Override
	public Iterator<ZFrame> iterator ()
	{
		// TODO Auto-generated method stub
		return this.frames.iterator ();
	}
	
	@Override
	public boolean offer (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		return this.frames.offer (e);
	}
	
	@Override
	public boolean offerFirst (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		return this.frames.offerFirst (e);
	}
	
	@Override
	public boolean offerLast (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		return this.frames.offerLast (e);
	}
	
	@Override
	public ZFrame peek ()
	{
		return this.frames.peek ();
	}
	
	@Override
	public ZFrame peekFirst ()
	{
		try {
			return this.frames.peekFirst ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public ZFrame peekLast ()
	{
		try {
			return this.frames.peekLast ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public ZFrame poll ()
	{
		return this.frames.poll ();
	}
	
	@Override
	public ZFrame pollFirst ()
	{
		return this.frames.pollFirst ();
	}
	
	@Override
	public ZFrame pollLast ()
	{
		return this.frames.pollLast ();
	}
	
	@Override
	public ZFrame pop ()
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		try {
			return this.frames.pop ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	/**
	 * Pop a ZFrame and return the toString() representation of it.
	 *
	 * @return toString version of pop'ed frame, or null if no frame exists.
	 */
	public String popString ()
	{
		final ZFrame frame = this.pop ();
		if (frame == null)
			return null;
		return frame.toString ();
	}
	
	public void push (final byte[] data)
	{
		this.push (new ZFrame (data));
	}
	
	public void push (final String str)
	{
		this.push (new ZFrame (str));
	}
	
	@Override
	public void push (final ZFrame e)
	{
		if (this.frames == null)
			this.frames = new ArrayDeque<ZFrame> ();
		this.frames.push (e);
	}
	
	@Override
	public ZFrame remove ()
	{
		return this.frames.remove ();
	}
	
	@Override
	public boolean remove (final Object o)
	{
		return this.frames.remove (o);
	}
	
	@Override
	public boolean removeAll (final Collection<?> arg0)
	{
		return this.frames.removeAll (arg0);
	}
	
	@Override
	public ZFrame removeFirst ()
	{
		try {
			return this.frames.removeFirst ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public boolean removeFirstOccurrence (final Object o)
	{
		return this.frames.removeFirstOccurrence (o);
	}
	
	@Override
	public ZFrame removeLast ()
	{
		try {
			return this.frames.removeLast ();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}
	
	@Override
	public boolean removeLastOccurrence (final Object o)
	{
		return this.frames.removeLastOccurrence (o);
	}
	
	@Override
	public boolean retainAll (final Collection<?> arg0)
	{
		return this.frames.retainAll (arg0);
	}
	
	/**
	 * Send message to 0MQ socket.
	 *
	 * @param socket
	 *              0MQ socket to send ZMsg on.
	 */
	public void send (final Socket socket)
	{
		this.send (socket, false);
	}
	
	/**
	 * Send message to 0MQ socket, destroys contents after sending if destroy param is set to true.
	 * If the message has no frames, sends nothing but still destroy()s the ZMsg object
	 * @param socket
	 * 				0MQ socket to send ZMsg on.
	 */
	public void send (final Socket socket, final boolean destroy)
	{
		if (socket == null)
			throw new IllegalArgumentException ("socket is null");
		if (this.frames.size () == 0)
			return;
		final Iterator<ZFrame> i = this.frames.iterator ();
		while (i.hasNext ()) {
			final ZFrame f = i.next ();
			f.sendAndKeep (socket, (i.hasNext ()) ? ZMQ.SNDMORE : 0);
		}
		if (destroy) {
			this.destroy ();
		}
	}
	
	@Override
	public int size ()
	{
		return this.frames.size ();
	}
	
	@Override
	public Object[] toArray ()
	{
		return this.frames.toArray ();
	}
	
	@Override
	public <T> T[] toArray (final T[] arg0)
	{
		return this.frames.toArray (arg0);
	}
	
	/**
	 * Pop frame off front of message, caller now owns frame.
	 * If next frame is empty, pops and destroys that empty frame
	 * (e.g. useful when unwrapping ROUTER socket envelopes)
	 * @return
	 * 			Unwrapped frame
	 */
	public ZFrame unwrap ()
	{
		if (this.size () == 0)
			return null;
		final ZFrame f = this.pop ();
		ZFrame empty = this.getFirst ();
		if (empty.hasData () && (empty.size () == 0)) {
			empty = this.pop ();
			empty.destroy ();
		}
		return f;
	}
	
	/**
	 * Push frame plus empty frame to front of message, before 1st frame.
	 * Message takes ownership of frame, will destroy it when message is sent.
	 * @param frame
	 */
	public void wrap (final ZFrame frame)
	{
		if (frame != null) {
			this.push (new ZFrame (""));
			this.push (frame);
		}
	}
	
	/**
	 * Load / append a ZMsg from an open DataInputStream
	 * 
	 * @param file
	 * 			DataInputStream connected to file
	 * @return
	 * 			ZMsg object
	 */
	public static ZMsg load (final DataInputStream file)
	{
		if (file == null)
			return null;
		final ZMsg rcvMsg = new ZMsg ();
		try {
			final int msgSize = file.readInt ();
			if (msgSize > 0) {
				int msgNbr = 0;
				while (++msgNbr <= msgSize) {
					final int frameSize = file.readInt ();
					final byte[] data = new byte[frameSize];
					file.read (data);
					rcvMsg.add (new ZFrame (data));
				}
			}
			return rcvMsg;
		} catch (final IOException e) {
			return null;
		}
	}
	
	/**
	 * Create a new ZMsg from one or more Strings
	 *
	 * @param strings
	 *      Strings to add as frames.
	 * @return
	 *      ZMsg object
	 */
	public static ZMsg newStringMsg (final String ... strings)
	{
		final ZMsg msg = new ZMsg ();
		for (final String data : strings) {
			msg.addString (data);
		}
		return msg;
	}
	
	/**
	 * Receives message from socket, returns ZMsg object or null if the
	 * recv was interrupted. Does a blocking recv, if you want not to block then use
	 * the ZLoop class or ZMQ.Poller to check for socket input before receiving or recvMsg with flag ZMQ.DONTWAIT.
	 * @param	socket
	 * @return
	 */
	public static ZMsg recvMsg (final Socket socket)
	{
		return ZMsg.recvMsg (socket, 0);
	}
	
	/**
	 * Receives message from socket, returns ZMsg object or null if the
	 * recv was interrupted. Does a blocking recv, if you want not to block then use
	 * the ZLoop class or ZMQ.Poller to check for socket input before receiving.
	 * @param	socket
	 * @param   flag see ZMQ constants
	 * @return
	 */
	public static ZMsg recvMsg (final Socket socket, final int flag)
	{
		if (socket == null)
			throw new IllegalArgumentException ("socket is null");
		final ZMsg msg = new ZMsg ();
		while (true) {
			final ZFrame f = ZFrame.recvFrame (socket, flag);
			if (f == null) {
				// If receive failed or was interrupted
				msg.destroy ();
				break;
			}
			msg.add (f);
			if (!f.hasMore ())
				break;
		}
		return msg;
	}
	
	/**
	 * Save message to an open data output stream.
	 * 
	 * Data saved as:
	 * 		4 bytes: number of frames
	 * 	For every frame:
	 * 		4 bytes: byte size of frame data
	 * 		+ n bytes: frame byte data
	 * 
	 * @param msg
	 * 			ZMsg to save
	 * @param file
	 * 			DataOutputStream
	 * @return
	 * 			True if saved OK, else false
	 */
	public static boolean save (final ZMsg msg, final DataOutputStream file)
	{
		if (msg == null)
			return false;
		try {
			// Write number of frames
			file.writeInt (msg.size ());
			if (msg.size () > 0) {
				for (final ZFrame f : msg) {
					// Write byte size of frame
					file.writeInt (f.size ());
					// Write frame byte data
					file.write (f.getData ());
				}
			}
			return true;
		} catch (final IOException e) {
			return false;
		}
	}
	
	/**
	 * Hold internal list of ZFrame objects
	 */
	private ArrayDeque<ZFrame> frames;
}
