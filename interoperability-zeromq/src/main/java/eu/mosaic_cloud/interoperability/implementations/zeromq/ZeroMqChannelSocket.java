/*
 * #%L
 * mosaic-interoperability-zeromq
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.interoperability.implementations.zeromq;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;
import org.zeromq.ZMQ;


public final class ZeroMqChannelSocket
		extends Object
{
	private ZeroMqChannelSocket (final String self, final Runnable dequeueTrigger, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (self);
		Preconditions.checkNotNull (threading);
		this.threading = threading;
		this.transcript = Transcript.create (this);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
		this.self = self;
		this.inboundPackets = new LinkedBlockingQueue<ZeroMqChannelPacket> ();
		this.outboundPackets = new LinkedBlockingQueue<ZeroMqChannelPacket> ();
		this.dequeueTrigger = dequeueTrigger;
		this.shouldStop = false;
		this.loop = Threading.createAndStartDaemonThread (ZeroMqChannelSocket.this.threading, ZeroMqChannelSocket.this, "loop", new Loop ());
	}
	
	public final void accept (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		this.transcript.traceDebugging ("accepting on `%s`...", endpoint);
		if (this.socket == null)
			Threading.sleep (ZeroMqChannelSocket.defaultDelay);
		if (this.socket == null)
			throw (new IllegalStateException ());
		this.socket.bind (endpoint);
		Threading.sleep (ZeroMqChannelSocket.defaultDelay);
	}
	
	public final void connect (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		this.transcript.traceDebugging ("connecting to `%s`...", endpoint);
		if (this.socket == null)
			Threading.sleep (ZeroMqChannelSocket.defaultDelay);
		if (this.socket == null)
			throw (new IllegalStateException ());
		this.socket.connect (endpoint);
		Threading.sleep (ZeroMqChannelSocket.defaultDelay);
	}
	
	public final ZeroMqChannelPacket dequeue (final long timeout)
	{
		return (Threading.poll (this.inboundPackets, timeout));
	}
	
	public final boolean enqueue (final ZeroMqChannelPacket packet, final long timeout)
	{
		return (Threading.offer (this.outboundPackets, packet, timeout));
	}
	
	public final void terminate ()
	{
		this.terminate (0);
	}
	
	public final boolean terminate (final long timeout)
	{
		this.transcript.traceDebugging ("terminating...");
		this.shouldStop = true;
		return (Threading.join (this.loop, timeout));
	}
	
	final void loop ()
	{
		this.transcript.traceDebugging ("loopping...");
		final ZMQ.Poller poller = ZeroMqChannelSocket.defaultContext.poller (3);
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
			if (!this.outboundPackets.isEmpty ())
				outboundPollIndex = poller.register (this.socket, ZMQ.Poller.POLLOUT);
			else
				outboundPollIndex = -1;
			errorPollIndex = poller.register (this.socket, ZMQ.Poller.POLLERR);
			// FIXME
			if (poller.poll (5 * 1000) > 0) {
				if (((errorPollIndex >= 0) && poller.pollerr (errorPollIndex)) || ((this.socket.getEvents () & ZMQ.Poller.POLLERR) != 0))
					this.failed ();
				else {
					if ((inboundPollIndex >= 0) && poller.pollin (inboundPollIndex))
						this.receive ();
					if ((outboundPollIndex >= 0) && poller.pollout (outboundPollIndex))
						this.send ();
				}
			}
			if (inboundPollIndex != -1)
				poller.unregister (this.socket);
			if (outboundPollIndex != -1)
				poller.unregister (this.socket);
			if (errorPollIndex != -1)
				poller.unregister (this.socket);
		}
	}
	
	final void setup ()
	{
		this.transcript.traceDebugging ("setting-up...");
		this.socket = ZeroMqChannelSocket.defaultContext.socket (ZMQ.XREP);
		this.socket.setIdentity (this.self.getBytes ());
	}
	
	final void teardown ()
	{
		this.transcript.traceDebugging ("tearing-down...");
		this.socket.close ();
		this.socket = null;
	}
	
	private final void failed ()
	{
		this.transcript.traceError ("socket failed; ignoring!");
	}
	
	private final void receive ()
	{
		this.transcript.traceDebugging ("receiving packet...");
		final String peer;
		final byte[] peer_;
		final byte[] delimiter;
		final byte[] header;
		final byte[] payload;
		peer_ = this.socket.recv (0);
		if (peer_ == null) {
			this.transcript.traceError ("error encountered while receiving packet peer part; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (!this.socket.hasReceiveMore ()) {
			this.transcript.traceError ("error encountered while decoding packet: missing delimiter; ignoring!");
			this.receiveFlush ();
			return;
		}
		delimiter = this.socket.recv (0);
		if (delimiter == null) {
			this.transcript.traceError ("error encountered while receiving packet delimiter part: ignoring!");
			this.receiveFlush ();
			return;
		}
		if (delimiter.length != 0) {
			this.transcript.traceError ("error encountered while decoding packet: non-empty delimiter; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (!this.socket.hasReceiveMore ()) {
			this.transcript.traceError ("error encounterd while decoding packet: missing header; ignoring!");
			this.receiveFlush ();
			return;
		}
		header = this.socket.recv (0);
		if (header == null) {
			this.transcript.traceError ("error encountered while receiving packet header part; ignoring!");
			this.receiveFlush ();
			return;
		}
		if (this.socket.hasReceiveMore ()) {
			payload = this.socket.recv (0);
			if (payload == null) {
				this.transcript.traceError ("error encountered while receiving packet payload part; ignoring!");
				this.receiveFlush ();
				return;
			}
		} else
			payload = null;
		if (this.socket.hasReceiveMore ()) {
			this.transcript.traceError ("error encountered while receiving packet: unexpected garbage; ignoring!");
			this.receiveFlush ();
			return;
		}
		peer = new String (peer_);
		final ZeroMqChannelPacket packet = new ZeroMqChannelPacket (peer, ByteBuffer.wrap (header), payload != null ? ByteBuffer.wrap (payload) : null);
		if (!Threading.offer (this.inboundPackets, packet, -1))
			throw (new BufferOverflowException ());
		if (this.dequeueTrigger != null)
			try {
				this.dequeueTrigger.run ();
			} catch (final Error exception) {
				this.transcript.traceError ("error encountered while executing dequeue trigger; ignoring!");
			}
	}
	
	private final void receiveFlush ()
	{
		while (this.socket.hasReceiveMore ())
			this.socket.recv (0);
	}
	
	private final void send ()
	{
		this.transcript.traceDebugging ("sending packet...");
		final ZeroMqChannelPacket packet = this.outboundPackets.remove ();
		if (!this.socket.send (packet.peer.getBytes (), ZMQ.SNDMORE)) {
			this.transcript.traceError ("error encountered while sending packet: ignoring!");
			return;
		}
		if (!this.socket.send (new byte[0], ZMQ.SNDMORE)) {
			this.transcript.traceError ("error encountered while sending packet: ignoring!");
			return;
		}
		final byte[] header = new byte[packet.header.remaining ()];
		packet.header.get (header);
		final byte[] payload;
		if (packet.payload != null) {
			payload = new byte[packet.payload.remaining ()];
			packet.payload.get (payload);
		} else
			payload = null;
		if (!this.socket.send (header, (payload != null) ? ZMQ.SNDMORE : 0)) {
			this.transcript.traceError ("error encountered while sending packet: ignoring!");
			return;
		}
		if (payload != null)
			if (!this.socket.send (payload, 0)) {
				this.transcript.traceError ("error encountered while sending packet: ignoring!");
				return;
			}
	}
	
	final Runnable dequeueTrigger;
	final TranscriptExceptionTracer exceptions;
	final LinkedBlockingQueue<ZeroMqChannelPacket> inboundPackets;
	final Thread loop;
	final LinkedBlockingQueue<ZeroMqChannelPacket> outboundPackets;
	final String self;
	volatile boolean shouldStop;
	ZMQ.Socket socket;
	final ThreadingContext threading;
	final Transcript transcript;
	
	public static final ZeroMqChannelSocket create (final String self, final Runnable dequeueTrigger, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new ZeroMqChannelSocket (self, dequeueTrigger, threading, exceptions));
	}
	
	public static final long defaultDelay = 50;
	static final ZMQ.Context defaultContext = ZMQ.context (1);
	
	private final class Loop
			extends Object
			implements
				Runnable
	{
		Loop ()
		{
			super ();
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannelSocket.this.setup ();
			ZeroMqChannelSocket.this.loop ();
			ZeroMqChannelSocket.this.teardown ();
		}
	}
}
