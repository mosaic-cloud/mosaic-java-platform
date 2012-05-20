
package org.zeromq;


import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;


/**
 * ZeroMQ Queue Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQQueue
		implements
			Runnable
{
	/**
	 * Class constructor.
	 * 
	 * @param context
	 *            a 0MQ context previously created.
	 * @param inSocket
	 *            input socket
	 * @param outSocket
	 *            output socket
	 */
	public ZMQQueue (final Context context, final Socket inSocket, final Socket outSocket)
	{
		this.inSocket = inSocket;
		this.outSocket = outSocket;
		this.poller = context.poller (2);
		this.poller.register (inSocket, ZMQ.Poller.POLLIN);
		this.poller.register (outSocket, ZMQ.Poller.POLLIN);
	}
	
	/**
	 * Queuing of requests and replies.
	 */
	@Override
	public void run ()
	{
		byte[] msg = null;
		boolean more = true;
		while (!Thread.currentThread ().isInterrupted ()) {
			try {
				// wait while there are either requests or replies to process
				if (this.poller.poll (250000) < 1) {
					continue;
				}
				// process a request
				if (this.poller.pollin (0)) {
					more = true;
					while (more) {
						msg = this.inSocket.recv (0);
						more = this.inSocket.hasReceiveMore ();
						if (msg != null) {
							this.outSocket.send (msg, more ? ZMQ.SNDMORE : 0);
						}
					}
				}
				// process a reply
				if (this.poller.pollin (1)) {
					more = true;
					while (more) {
						msg = this.outSocket.recv (0);
						more = this.outSocket.hasReceiveMore ();
						if (msg != null) {
							this.inSocket.send (msg, more ? ZMQ.SNDMORE : 0);
						}
					}
				}
			} catch (final ZMQException e) {
				// context destroyed, exit
				if (ZMQ.Error.ETERM.getCode () == e.getErrorCode ()) {
					break;
				}
				throw e;
			}
		}
	}
	
	private final ZMQ.Socket inSocket;
	private final ZMQ.Socket outSocket;
	private final ZMQ.Poller poller;
}
