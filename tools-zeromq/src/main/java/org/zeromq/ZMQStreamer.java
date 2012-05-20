
package org.zeromq;


import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;


/**
 * ZeroMQ Streamer Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQStreamer
		extends ZMQForwarder
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
	public ZMQStreamer (final Context context, final Socket inSocket, final Socket outSocket)
	{
		super (context, inSocket, outSocket);
	}
}
