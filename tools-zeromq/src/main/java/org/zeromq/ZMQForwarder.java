
package org.zeromq;


import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;


/**
 * ZeroMQ Forwarder Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQForwarder
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
	public ZMQForwarder (final Context context, final Socket inSocket, final Socket outSocket)
	{
		this.inSocket = inSocket;
		this.outSocket = outSocket;
		this.poller = context.poller (1);
		this.poller.register (inSocket, ZMQ.Poller.POLLIN);
	}
	
	/**
	 * Forwarding messages.
	 */
	@Override
	public void run ()
	{
		byte[] msg = null;
		boolean more = true;
		while (!Thread.currentThread ().isInterrupted ()) {
			try {
				// wait while there are requests to process
				if (this.poller.poll (250000) < 1) {
					continue;
				}
				msg = this.inSocket.recv (0);
				more = this.inSocket.hasReceiveMore ();
				if (msg != null) {
					this.outSocket.send (msg, more ? ZMQ.SNDMORE : 0);
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
