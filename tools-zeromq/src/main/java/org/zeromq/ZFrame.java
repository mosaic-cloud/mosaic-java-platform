
package org.zeromq;


import java.util.Arrays;

import org.zeromq.ZMQ.Socket;


/**
 * ZFrame
 * 
 * @author rsmith (at) rsbatechnology (dot) co (dot) uk
 * 
 * The ZFrame class provides methods to send and receive single message
 * frames across 0MQ sockets. A 'frame' corresponds to one underlying zmq_msg_t in the libzmq code.
 * When you read a frame from a socket, the more() method indicates if the frame is part of an 
 * unfinished multipart message.  The send() method normally destroys the frame, but with the ZFRAME_REUSE flag, you can send
 * the same frame many times. Frames are binary, and this class has no special support for text data.
 * 
 * Based on <a href="http://github.com/zeromq/czmq/blob/master/src/zframe.c">zframe.c</a> in czmq
 * 
 */
public class ZFrame
{
	/**
	 * Class Constructor
	 * Copies message data into ZFrame object
	 * @param data
	 * 			Data to copy into ZFrame object
	 */
	public ZFrame (final byte[] data)
	{
		if (data != null) {
			this.data = (byte[]) data.clone ();
		}
	}
	
	/**
	 * Class Constructor
	 * Copies String into frame data
	 * @param data
	 */
	public ZFrame (final String data)
	{
		if (data != null) {
			this.data = data.getBytes ();
		}
	}
	
	/**
	 * Class Constructor
	 * Creates an empty frame.
	 * (Useful when reading frames from a 0MQ Socket)
	 */
	protected ZFrame ()
	{
		// Empty constructor
	}
	
	/**
	 * Destructor.
	 */
	public void destroy ()
	{
		if (this.hasData ())
			this.data = null;
	}
	
	/**
	 * Creates a new frame that duplicates an existing frame
	 * @return
	 *			Duplicate of frame; message contents copied into new byte array
	 */
	public ZFrame duplicate ()
	{
		return new ZFrame (this.data.clone ());
	}
	
	@Override
	public boolean equals (final Object o)
	{
		if (this == o)
			return true;
		if ((o == null) || (this.getClass () != o.getClass ()))
			return false;
		final ZFrame zFrame = (ZFrame) o;
		if (!Arrays.equals (this.data, zFrame.data))
			return false;
		return true;
	}
	
	/**
	 * @return the data
	 */
	public byte[] getData ()
	{
		return this.data;
	}
	
	/**
	 * Convenience method to ascertain if this frame contains some message data
	 * @return
	 * 			True if frame contains data
	 */
	public boolean hasData ()
	{
		return this.data != null;
	}
	
	@Override
	public int hashCode ()
	{
		return this.data != null ? Arrays.hashCode (this.data) : 0;
	}
	
	/**
	 * @return More flag, true if last read had MORE message parts to come
	 */
	public boolean hasMore ()
	{
		return this.more;
	}
	
	/**
	 * Returns true if both frames have byte - for byte identical data
	 * @param other
	 * 			The other ZFrame to compare
	 * @return
	 * 			True if both ZFrames have same byte-identical data, else false
	 */
	public boolean hasSameData (final ZFrame other)
	{
		if (other == null)
			return false;
		if (this.size () == other.size ()) {
			if (this.hasData () && other.hasData ()) {
				for (int i = 0; i < this.size (); i++) {
					if (this.data[i] != other.data[i])
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets new contents for frame
	 * @param data
	 * 			New byte array contents for frame
	 */
	public void reset (final byte[] data)
	{
		this.data = data;
	}
	
	/**
	 * Sends frame to socket if it contains data.
	 * Use this method to send an isolated frame and destroy the data after.
	 * Uses default behaviour of Socket.send() method, with no flags set
	 * @param socket
	 * 			0MQ socket to send frame
	 */
	public void sendAndDestroy (final Socket socket)
	{
		this.sendAndDestroy (socket, 0);
	}
	
	/**
	 * Sends frame to socket if it contains data.
	 * Use this method to send a frame and destroy the data after.
	 * @param socket
	 * 			0MQ socket to send frame
	 * @param flags
	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class	
	 */
	public void sendAndDestroy (final Socket socket, final int flags)
	{
		this.send (socket, flags);
		this.destroy ();
	}
	
	/**
	 * Sends frame to socket if it contains any data.
	 * Frame contents are kept after the send.
	 * Uses default behaviour of Socket.send() method, with no flags set
	 * @param socket	
	 * 			0MQ socket to send frame
	 */
	public void sendAndKeep (final Socket socket)
	{
		this.sendAndKeep (socket, 0);
	}
	
	/**
	 * Sends frame to socket if it contains any data.
	 * Frame contents are kept after the send.
	 * @param socket	
	 * 			0MQ socket to send frame
	 * @param flags
	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class	
	 */
	public void sendAndKeep (final Socket socket, final int flags)
	{
		this.send (socket, flags);
	}
	
	/**
	 * Returns byte size of frame, if set, else 0
	 * @return
	 * 			Number of bytes in frame data, else 0
	 */
	public int size ()
	{
		if (this.hasData ())
			return this.data.length;
		else
			return 0;
	}
	
	/**
	 * String equals.
	 * Uses String compareTo for the comparison (lexigraphical)
	 * @param str
	 * 			String to compare with frame data
	 * @return
	 * 			True if frame body data matches given string
	 */
	public boolean streq (final String str)
	{
		if (!this.hasData ())
			return false;
		return new String (this.data).compareTo (str) == 0;
	}
	
	/**
	 * Returns frame data as a printable hex string
	 * @return
	 */
	public String strhex ()
	{
		final String hexChar = "0123456789ABCDEF";
		final StringBuilder b = new StringBuilder ();
		for (final byte element : this.data) {
			final int b1 = (element >>> 4) & 0xf;
			final int b2 = element & 0xf;
			b.append (hexChar.charAt (b1));
			b.append (hexChar.charAt (b2));
		}
		return b.toString ();
	}
	
	/**
	 * Returns a human - readable representation of frame's data
	 * @return
	 * 			A text string or hex-encoded string if data contains any non-printable ASCII characters
	 */
	@Override
	public String toString ()
	{
		if (!this.hasData ())
			return null;
		// Dump message as text or hex-encoded string
		boolean isText = true;
		for (final byte element : this.data) {
			if ((element < 32) || (element > 127))
				isText = false;
		}
		if (isText)
			return new String (this.data);
		else
			return this.strhex ();
	}
	
	/**
	 * Internal method to call recv on the socket.
	 * Does not trap any ZMQExceptions but expects caling routine to handle them.
	 * @param socket
	 * 			0MQ socket to read from
	 * @return
	 * 			Byte array
	 */
	private byte[] recv (final Socket socket, final int flags)
	{
		if (socket == null)
			throw new IllegalArgumentException ("socket parameter must not be null");
		this.data = socket.recv (flags);
		this.more = socket.hasReceiveMore ();
		return this.data;
	}
	
	/**
	 * Internal method to call org.zeromq.Socket send() method.
	 * @param socket
	 * 			0MQ socket to send on
	 * @param flags
	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class
	 */
	private void send (final Socket socket, final int flags)
	{
		if (socket == null) {
			throw new IllegalArgumentException ("socket parameter must be set");
		}
		if (!this.hasData ()) {
			throw new IllegalAccessError ("Cannot send frame without data");
		}
		// Note the jzmq Socket.cpp JNI class does a memcpy of the byte data before calling
		// the 0MQ send function, so don't have to clone the message data again here.
		socket.send (this.data, flags);
	}
	
	/**
	 * Receives single frame from socket, returns the received frame object, or null if the recv
	 * was interrupted. Does a blocking recv, if you want to not block then use
	 * recvFrame(socket, ZMQ.DONTWAIT);
	 * 
	 * @param	socket
	 * 				Socket to read from
	 * @return  
	 * 				received frame, else null
	 */
	public static ZFrame recvFrame (final Socket socket)
	{
		final ZFrame f = new ZFrame ();
		f.recv (socket, 0);
		return f;
	}
	
	/**
	 * Receive a new frame off the socket, Returns newly-allocated frame, or
	 * null if there was no input waiting, or if the read was interrupted.
	 * @param	socket
	 * 				Socket to read from
	 * @param	flags
	 * 				Pass flags to 0MQ socket.recv call
	 * @return  
	 * 				received frame, else null
	 */
	public static ZFrame recvFrame (final Socket socket, final int flags)
	{
		final ZFrame f = new ZFrame ();
		f.recv (socket, flags);
		return f;
	}
	
	private byte[] data;
	private boolean more;
}
