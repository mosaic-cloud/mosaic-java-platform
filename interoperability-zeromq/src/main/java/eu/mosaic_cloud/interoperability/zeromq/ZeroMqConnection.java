
package eu.mosaic_cloud.interoperability.zeromq;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;


public final class ZeroMqConnection
		extends Object
{
	public ZeroMqConnection (final String self, final Runnable dequeueTrigger)
	{
		super ();
		Preconditions.checkNotNull (self);
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.self = self;
		this.inboundPackets = new LinkedBlockingQueue<Packet> ();
		this.outboundPackets = new LinkedBlockingQueue<Packet> ();
		this.dequeueTrigger = dequeueTrigger;
		this.shouldStop = false;
		this.looper = new Looper ();
		this.looper.setDaemon (true);
		this.looper.start ();
	}
	
	public final void accept (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		this.logger.debug ("accepting on `{}`...", endpoint);
		if (this.socket == null)
			try {
				Thread.sleep (100);
			} catch (final InterruptedException exception) {}
		if (this.socket == null)
			throw (new IllegalStateException ());
		this.socket.bind (endpoint);
	}
	
	public final void connect (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		this.logger.debug ("connecting to `{}`...", endpoint);
		if (this.socket == null)
			try {
				Thread.sleep (100);
			} catch (final InterruptedException exception) {}
		if (this.socket == null)
			throw (new IllegalStateException ());
		this.socket.connect (endpoint);
	}
	
	public final Packet dequeue ()
	{
		return (this.inboundPackets.poll ());
	}
	
	public final Packet dequeue (final long timeout)
	{
		try {
			return (this.inboundPackets.poll (timeout, TimeUnit.MILLISECONDS));
		} catch (final InterruptedException exception) {
			return (null);
		}
	}
	
	public final boolean enqueue (final Packet packet)
	{
		return (this.outboundPackets.offer (packet));
	}
	
	public final boolean enqueue (final Packet packet, final long timeout)
	{
		try {
			return (this.outboundPackets.offer (packet, timeout, TimeUnit.MILLISECONDS));
		} catch (final InterruptedException exception) {
			return (false);
		}
	}
	
	public final void terminate ()
	{
		this.logger.debug ("terminating...");
		this.shouldStop = true;
	}
	
	private final void failed ()
	{
		this.logger.error ("socket failed; ignoring!");
	}
	
	private final void loop ()
	{
		this.logger.debug ("loopping...");
		final ZMQ.Poller poller = ZeroMqConnection.context.poller (3);
		while (true) {
			if (this.shouldStop)
				break;
			final int inboundPollIndex;
			final int outboundPollIndex;
			final int errorPollIndex;
			if (this.inboundPackets.remainingCapacity () > 0)
				inboundPollIndex = poller.register (this.socket, ZMQ.Poller.POLLIN);
			else
				inboundPollIndex = -1;
			if (this.outboundPackets.size () > 0)
				outboundPollIndex = poller.register (this.socket, ZMQ.Poller.POLLOUT);
			else
				outboundPollIndex = -1;
			errorPollIndex = poller.register (this.socket, ZMQ.Poller.POLLERR);
			if (poller.poll (100 * 1000) > 0) {
				if (poller.pollerr (errorPollIndex) || ((this.socket.getEvents () & ZMQ.Poller.POLLERR) != 0))
					this.failed ();
				if ((inboundPollIndex >= 0) && poller.pollin (inboundPollIndex))
					this.receive ();
				if ((outboundPollIndex >= 0) && poller.pollout (outboundPollIndex))
					this.send ();
			}
			poller.unregister (this.socket);
		}
	}
	
	private final void receive ()
	{
		this.logger.trace ("receiving packet...");
		final String peer;
		final byte[] peer_;
		final byte[] delimiter;
		final byte[] header;
		final byte[] payload;
		peer_ = this.socket.recv (0);
		if (peer_ == null) {
			this.logger.error ("error encountered while receiving packet peer part; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (!this.socket.hasReceiveMore ()) {
			this.logger.error ("error encountered while decoding packet: missing delimiter; ignoring!");
			this.receiveFlush ();
			return;
		}
		delimiter = this.socket.recv (0);
		if (delimiter == null) {
			this.logger.error ("error encountered while receiving packet delimiter part: ignoring!");
			this.receiveFlush ();
			return;
		}
		if (delimiter.length != 0) {
			this.logger.error ("error encountered while decoding packet: non-empty delimiter; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (!this.socket.hasReceiveMore ()) {
			this.logger.error ("error encounterd while decoding packet: missing header; ignoring!");
			this.receiveFlush ();
			return;
		}
		header = this.socket.recv (0);
		if (header == null) {
			this.logger.error ("error encountered while receiving packet header part; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (this.socket.hasReceiveMore ()) {
			payload = this.socket.recv (0);
			if (payload == null) {
				this.logger.error ("error encountered while receiving packet payload part; ignoring!");
				this.receiveFlush ();
				return;
			}
		} else
			payload = null;
		if (this.socket.hasReceiveMore ()) {
			this.logger.error ("error encountered while receiving packet: unexpected garbage; ignoring!");
			this.receiveFlush ();
			return;
		}
		peer = new String (peer_);
		final Packet packet = new Packet (peer, header, payload);
		this.inboundPackets.add (packet);
		if (this.dequeueTrigger != null)
			try {
				this.dequeueTrigger.run ();
			} catch (final Error exception) {
				this.logger.error ("error encountered while executing dequeue trigger; ignoring!");
			}
	}
	
	private final void receiveFlush ()
	{
		while (this.socket.hasReceiveMore ())
			this.socket.recv (0);
	}
	
	private final void send ()
	{
		this.logger.trace ("sending packet...");
		final Packet packet = this.outboundPackets.remove ();
		if (!this.socket.send (packet.peer.getBytes (), ZMQ.SNDMORE)) {
			this.logger.error ("error encountered while sending packet: ignoring!");
			return;
		}
		if (!this.socket.send (new byte[0], ZMQ.SNDMORE)) {
			this.logger.error ("error encountered while sending packet: ignoring!");
			return;
		}
		if (!this.socket.send (packet.header, (packet.payload != null) ? ZMQ.SNDMORE : 0)) {
			this.logger.error ("error encountered while sending packet: ignoring!");
			return;
		}
		if (packet.payload != null)
			if (!this.socket.send (packet.payload, 0)) {
				this.logger.error ("error encountered while sending packet: ignoring!");
				return;
			}
	}
	
	private final void setup ()
	{
		this.logger.debug ("setting-up...");
		this.socket = ZeroMqConnection.context.socket (ZMQ.XREP);
		this.socket.setIdentity (this.self.getBytes ());
	}
	
	private final void teardown ()
	{
		this.logger.debug ("tearing-down...");
		this.socket.close ();
		this.socket = null;
	}
	
	private final Runnable dequeueTrigger;
	private final LinkedBlockingQueue<Packet> inboundPackets;
	private final Logger logger;
	private final Thread looper;
	private final LinkedBlockingQueue<Packet> outboundPackets;
	private final String self;
	private boolean shouldStop;
	private ZMQ.Socket socket;
	
	static {
		context = ZMQ.context (1);
	}
	
	private static final ZMQ.Context context;
	
	public static final class Packet
			extends Object
	{
		public Packet (final String peer, final byte[] header, final byte[] payload)
		{
			super ();
			Preconditions.checkNotNull (peer);
			Preconditions.checkNotNull (header);
			this.peer = peer;
			this.header = header;
			this.payload = payload;
		}
		
		public final byte[] header;
		public final byte[] payload;
		public final String peer;
	}
	
	private final class Looper
			extends Thread
	{
		@Override
		public final void run ()
		{
			ZeroMqConnection.this.setup ();
			ZeroMqConnection.this.loop ();
			ZeroMqConnection.this.teardown ();
		}
	}
}
