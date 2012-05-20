
package org.zeromq;


import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;


/**
 * ZContext provides a high-level ZeroMQ context management class
 * @see http://github.com/zeromq/czmq/blob/master/src/zctx.c
 * 
 * @author rsmith (at) rsbatechnology (dot) co (dot) uk
 *
 * The ZContext class wraps java org.zeromq.Context objects, which in turn wrap native 0MQ contexts.
 * It manages open sockets in the context and automatically closes these before terminating the context. 
 * It provides a simple way to set the linger timeout on sockets, and configure contexts for number of I/O threads. 
 * Sets-up signal (interrrupt) handling for the process.
 * 
 */
public class ZContext
{
	/**
	 * Class Constructor
	 */
	public ZContext ()
	{
		this.context = null; // Don't create Context until create 1st 0MQ socket
		this.sockets = new CopyOnWriteArrayList<Socket> ();
		this.ioThreads = 1;
		this.linger = 0;
		this.main = true;
	}
	
	/**
	 * Creates a new managed socket within this ZContext instance.
	 * Use this to get automatic management of the socket at shutdown
	 * @param type
	 * 			socket type (see ZMQ static class members)
	 * @return
	 * 			Newly created Socket object
	 */
	public Socket createSocket (final int type)
	{
		if (this.context == null)
			this.context = ZMQ.context (this.ioThreads);
		// Create and register socket
		final Socket socket = this.context.socket (type);
		this.sockets.add (socket);
		return socket;
	}
	
	/**
	 * Destructor.  Call this to gracefully terminate context and close any managed 0MQ sockets
	 */
	public void destroy ()
	{
		final ListIterator<Socket> itr = this.sockets.listIterator ();
		while (itr.hasNext ()) {
			this.destroySocket (itr.next ());
		}
		this.sockets.clear ();
		// Only terminate context if we are on the main thread
		if (this.isMain () && (this.context != null))
			this.context.term ();
	}
	
	/**
	 * Destroys managed socket within this context
	 * and remove from sockets list
	 * @param s
	 * 			org.zeromq.Socket object to destroy
	 */
	public void destroySocket (final Socket s)
	{
		if (s == null)
			return;
		if (this.sockets.contains (s)) {
			s.setLinger (this.linger);
			s.close ();
			this.sockets.remove (s);
		}
	}
	
	/**
	 * @return the context
	 */
	public Context getContext ()
	{
		return this.context;
	}
	
	/**
	 * @return the ioThreads
	 */
	public int getIoThreads ()
	{
		return this.ioThreads;
	}
	
	/**
	 * @return the linger
	 */
	public int getLinger ()
	{
		return this.linger;
	}
	
	/**
	 * @return the sockets
	 */
	public List<Socket> getSockets ()
	{
		return this.sockets;
	}
	
	/**
	 * @return the main
	 */
	public boolean isMain ()
	{
		return this.main;
	}
	
	/**
	 * @param ctx	sets the underlying org.zeromq.Context associated with this ZContext wrapper object
	 */
	public void setContext (final Context ctx)
	{
		this.context = ctx;
	}
	
	/**
	 * @param ioThreads the ioThreads to set
	 */
	public void setIoThreads (final int ioThreads)
	{
		this.ioThreads = ioThreads;
	}
	
	/**
	 * @param linger the linger to set
	 */
	public void setLinger (final int linger)
	{
		this.linger = linger;
	}
	
	/**
	 * @param main the main to set
	 */
	public void setMain (final boolean main)
	{
		this.main = main;
	}
	
	/**
	 * Creates new shadow context.
	 * Shares same underlying org.zeromq.Context instance but has own list
	 * of managed sockets, io thread count etc. 
	 * @param ctx	Original ZContext to create shadow of
	 * @return	New ZContext
	 */
	public static ZContext shadow (final ZContext ctx)
	{
		final ZContext shadow = new ZContext ();
		shadow.setContext (ctx.getContext ());
		return shadow;
	}
	
	/**
	 * Reference to underlying Context object
	 */
	private ZMQ.Context context;
	/**
	 * Number of io threads allocated to this context, default 1
	 */
	private int ioThreads;
	/**
	 * Linger timeout, default 0
	 */
	private int linger;
	/**
	 * Indicates if context object is owned by main thread
	 * (useful for multi-threaded applications)
	 */
	private boolean main;
	/**
	 * List of sockets managed by this ZContext
	 */
	private final List<Socket> sockets;
}
