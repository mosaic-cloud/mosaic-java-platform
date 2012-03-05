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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class ZeroMqChannel
		extends Object
		implements
			Channel
{
	private ZeroMqChannel (final String self, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (self);
		Preconditions.checkNotNull (threading);
		this.transcript = Transcript.create (this);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
		this.threading = threading;
		this.selfIdentifier = self;
		this.state = new State ();
		this.handlers = new ConcurrentLinkedQueue<ZeroMqChannel.Handler> ();
		this.idle = new Semaphore (1);
		this.executor = this.threading.createCachedThreadPool (ThreadConfiguration.create (this, "zeromq-callbacks", true));
		this.socket = ZeroMqChannelSocket.create (this.selfIdentifier, new PacketDequeueTrigger (), this.threading, exceptions);
	}
	
	@Override
	public final void accept (final SessionSpecification specification, final SessionCallbacks callbacks)
	{
		Preconditions.checkNotNull (specification);
		Preconditions.checkNotNull (callbacks);
		final RoleSpecification selfRole = specification.getSelfRole ();
		final RoleSpecification peerRole = specification.getPeerRole ();
		final String selfRoleIdentifier = selfRole.getIdentifier ();
		final String peerRoleIdentifier = peerRole.getIdentifier ();
		Preconditions.checkNotNull (selfRoleIdentifier);
		Preconditions.checkNotNull (peerRoleIdentifier);
		final String acceptorKey = selfRoleIdentifier + "//" + peerRoleIdentifier;
		final Acceptor acceptor = new Acceptor (acceptorKey, selfRoleIdentifier, peerRoleIdentifier, specification, callbacks);
		synchronized (this.state.monitor) {
			if (this.state.acceptors.containsKey (acceptorKey)) {
				this.transcript.traceError ("error encountered while registering acceptor: already registered; throwing!");
				throw (new IllegalStateException ());
			}
			this.transcript.traceDebugging ("registering acceptor: `%s` -> %s...", acceptor.key, acceptor.callbacks);
			this.state.acceptors.put (acceptorKey, acceptor);
		}
	}
	
	public final void accept (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		synchronized (this.state.monitor) {
			this.socket.accept (endpoint);
		}
	}
	
	public final void connect (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		synchronized (this.state.monitor) {
			this.socket.connect (endpoint);
		}
	}
	
	@Override
	public final void connect (final String peer, final SessionSpecification specification, final Message message, final SessionCallbacks callbacks)
	{
		Preconditions.checkNotNull (peer);
		Preconditions.checkNotNull (specification);
		Preconditions.checkNotNull (callbacks);
		final RoleSpecification selfRole = specification.getSelfRole ();
		final RoleSpecification peerRole = specification.getPeerRole ();
		final String selfRoleIdentifier = selfRole.getIdentifier ();
		final String peerRoleIdentifier = peerRole.getIdentifier ();
		Preconditions.checkNotNull (selfRoleIdentifier);
		Preconditions.checkNotNull (peerRoleIdentifier);
		Preconditions.checkArgument (message.specification.getType () == MessageType.Initiation);
		final String sessionIdentifier = UUID.randomUUID ().toString ();
		synchronized (this.state.monitor) {
			final Session session = new Session (sessionIdentifier, selfRoleIdentifier, peerRoleIdentifier, peer, specification, callbacks, this.executor);
			this.state.sessions.put (sessionIdentifier, session);
			this.enqueueDispatcher (new SessionCreatedHandler (session));
			this.enqueueHandler (new PacketEnqueueHandler (session, message));
		}
	}
	
	@Override
	public final void register (final SessionSpecification specification)
	{
		Preconditions.checkNotNull (specification);
		final RoleSpecification selfRole = specification.getSelfRole ();
		final RoleSpecification peerRole = specification.getPeerRole ();
		final String selfRoleIdentifier = selfRole.getIdentifier ();
		final String peerRoleIdentifier = peerRole.getIdentifier ();
		Preconditions.checkNotNull (selfRoleIdentifier);
		Preconditions.checkNotNull (peerRoleIdentifier);
		final LinkedList<Coder> coders = new LinkedList<Coder> ();
		for (final MessageSpecification messageSpecification : specification.getMessages ()) {
			final String messageIdentifier = messageSpecification.getIdentifier ();
			final MessageType messageType = messageSpecification.getType ();
			final PayloadCoder messageCoder = messageSpecification.getPayloadCoder ();
			Preconditions.checkNotNull (messageIdentifier);
			Preconditions.checkNotNull (messageType);
			final String coderKey = selfRoleIdentifier + "//" + peerRoleIdentifier + "//" + messageIdentifier;
			final Coder coder = new Coder (coderKey, selfRoleIdentifier, peerRoleIdentifier, messageIdentifier, messageType, messageSpecification, messageCoder);
			coders.add (coder);
		}
		synchronized (this.state.monitor) {
			// FIXME
			// for (final Coder coder : coders)
			//	if (this.state.coders.containsKey (coder.key)) {
			//		this.transcript.traceError ("error encountered while registering coder: already registered; throwing!");
			//		throw (new IllegalStateException ());
			//	}
			for (final Coder coder : coders) {
				if (this.state.coders.containsKey (coder.key))
						continue;
				this.transcript.traceDebugging ("registering coder: `%s` -> %s...", coder.key, coder.coder);
				this.state.coders.put (coder.key, coder);
			}
		}
	}
	
	public final void terminate ()
	{
		this.terminate (0);
	}
	
	public final boolean terminate (final long timeout)
	{
		synchronized (this.state.monitor) {
			this.socket.terminate ();
			this.executor.shutdown ();
			return (Threading.join (this.executor, timeout));
		}
	}
	
	final void dispatchSessionCreated (final Session session)
	{
		session.callbacks.get ().created (session);
	}
	
	final void dispatchSessionDestroyed (final Session session)
	{
		session.callbacks.get ().destroyed (session);
	}
	
	final void dispatchSessionReceived (final Session session, final Message message)
	{
		session.callbacks.get ().received (session, message);
	}
	
	final void executeDispatcher (final Dispatcher dispatcher)
	{
		final Session session = dispatcher.session;
		session.dispatchContinued.set (Boolean.FALSE);
		try {
			dispatcher.dispatch ();
		} catch (final Error exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while executing dispatcher; ignoring!");
		}
		if (session.dispatchContinued.get () == Boolean.FALSE) {
			session.idle.release ();
			this.scheduleDispatcher (session);
		}
		session.dispatchContinued.set (null);
	}
	
	final void executeHandler (final Handler handler)
	{
		try {
			handler.handle ();
		} catch (final Error exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while executing handler; ignoring!");
		}
		this.idle.release ();
		this.scheduleHandler ();
	}
	
	final void executeTrigger (final Trigger trigger)
	{
		try {
			trigger.trigger ();
		} catch (final Error exception) {
			this.exceptions.traceIgnoredException (exception, "error encountered while executing trigger; ignoring!");
		}
	}
	
	final void handlePacketDequeue ()
	{
		synchronized (this.state.monitor) {
			final ZeroMqChannelPacket packet = this.socket.dequeue (0);
			if (packet == null)
				throw (new IllegalStateException ());
			final String sessionIdentifier;
			final String selfRoleIdentifier;
			final String peerRoleIdentifier;
			final String messageIdentifier;
			try {
				final DataInputStream stream = new DataInputStream (new ByteArrayInputStream (packet.header.array (), packet.header.arrayOffset () + packet.header.position (), packet.header.remaining ()));
				{
					final int bufferSize = stream.readUnsignedShort ();
					final byte[] buffer = new byte[bufferSize];
					stream.readFully (buffer);
					sessionIdentifier = new String (buffer);
				}
				{
					final int bufferSize = stream.readUnsignedShort ();
					final byte[] buffer = new byte[bufferSize];
					stream.readFully (buffer);
					selfRoleIdentifier = new String (buffer);
				}
				{
					final int bufferSize = stream.readUnsignedShort ();
					final byte[] buffer = new byte[bufferSize];
					stream.readFully (buffer);
					peerRoleIdentifier = new String (buffer);
				}
				{
					final int bufferSize = stream.readUnsignedShort ();
					final byte[] buffer = new byte[bufferSize];
					stream.readFully (buffer);
					messageIdentifier = new String (buffer);
				}
				if (stream.available () > 0) {
					this.transcript.traceError ("error encountered while decoding packet: header trailing garbage; ignoring!");
					return;
				}
				stream.close ();
			} catch (final IOException exception) {
				this.exceptions.traceIgnoredException (exception, "error encountered while decoding packet; ignoring!");
				return;
			}
			final String acceptorKey = selfRoleIdentifier + "//" + peerRoleIdentifier;
			final String coderKey = selfRoleIdentifier + "//" + peerRoleIdentifier + "//" + messageIdentifier;
			final Coder coder = this.state.coders.get (coderKey);
			if (coder == null) {
				this.transcript.traceError ("error encountered while decoding packet: missing coder; ignoring!");
				return;
			}
			final Session session;
			final Session existingSession = this.state.sessions.get (sessionIdentifier);
			if (existingSession != null) {
				session = existingSession;
			} else {
				final Acceptor acceptor = this.state.acceptors.get (acceptorKey);
				if (acceptor == null) {
					this.transcript.traceError ("error encountered while initiating session: mismatched roles; ignoring!");
					return;
				}
				if (coder.messageType != MessageType.Initiation) {
					this.transcript.traceError ("error encountered while initiating session: mismatched message type; ignoring!");
					return;
				}
				session = new Session (sessionIdentifier, selfRoleIdentifier, peerRoleIdentifier, packet.peer, acceptor.specification, acceptor.callbacks, this.executor);
				this.state.sessions.put (sessionIdentifier, session);
				this.enqueueDispatcher (new SessionCreatedHandler (session));
			}
			final Object payload;
			if ((coder.coder == null) && (packet.payload != null)) {
				this.transcript.traceError ("error encountered while decoding packet: missing coder, but existing payload; ignoring!");
				return;
			} else if ((coder.coder != null) && (packet.payload == null)) {
				this.transcript.traceError ("error encountered while decoding packet: existing coder, but missing payload; ignoring!");
				return;
			} else if (packet.payload != null)
				try {
					payload = coder.coder.decode (packet.payload);
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception, "error encountered while decoding packet: coder failed; ignoring!");
					return;
				}
			else
				payload = null;
			final Message message = new Message (coder.specification, payload);
			this.enqueueDispatcher (new SessionReceivedDispatcher (session, message));
			if (coder.messageType == MessageType.Termination)
				this.enqueueDispatcher (new SessionDestroyedDispatcher (session));
		}
	}
	
	final void handlePacketEnqueue (final Session session, final Message message)
	{
		synchronized (this.state.monitor) {
			final String messageIdentifier;
			try {
				messageIdentifier = message.specification.getIdentifier ();
			} catch (final Error exception) {
				this.exceptions.traceIgnoredException (exception, "error encountered while encoding packet; ignoring!");
				return;
			}
			final String coderKey = session.selfRoleIdentifier + "//" + session.peerRoleIdentifier + "//" + messageIdentifier;
			final Coder coder = this.state.coders.get (coderKey);
			if (coder == null) {
				this.transcript.traceError ("error encountered while decoding packet: missing coder; ignoring!");
				return;
			}
			final ByteBuffer payload;
			if ((coder.coder == null) && (message.payload != null)) {
				this.transcript.traceError ("error encountered while encoding packet: missing coder, but existing payload; ignoring!");
				return;
			} else if ((coder.coder != null) && (message.payload == null)) {
				this.transcript.traceError ("error encountered while encoding packet: existing coder, but missing payload; ignoring!");
				return;
			} else if (message.payload != null)
				try {
					payload = coder.coder.encode (message.payload);
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception, "error encountered while encoding packet: coder failed; ignoring!");
					return;
				}
			else
				payload = null;
			final byte[] header;
			try {
				final ByteArrayOutputStream headerStream = new ByteArrayOutputStream ();
				final DataOutputStream stream = new DataOutputStream (headerStream);
				{
					final byte[] buffer = session.sessionIdentifier.getBytes ();
					stream.writeShort (buffer.length);
					stream.write (buffer);
				}
				{
					final byte[] buffer = session.peerRoleIdentifier.getBytes ();
					stream.writeShort (buffer.length);
					stream.write (buffer);
				}
				{
					final byte[] buffer = session.selfRoleIdentifier.getBytes ();
					stream.writeShort (buffer.length);
					stream.write (buffer);
				}
				{
					final byte[] buffer = messageIdentifier.getBytes ();
					stream.writeShort (buffer.length);
					stream.write (buffer);
				}
				stream.close ();
				header = headerStream.toByteArray ();
			} catch (final IOException exception) {
				this.exceptions.traceIgnoredException (exception, "error encountered while encoding packet; ignoring!");
				return;
			}
			final ZeroMqChannelPacket packet = ZeroMqChannelPacket.create (session.peerIdentifier, ByteBuffer.wrap (header), payload);
			if (!this.socket.enqueue (packet, 1000))
				throw (new IllegalStateException ());
			if (coder.messageType == MessageType.Termination)
				this.enqueueDispatcher (new SessionDestroyedDispatcher (session));
		}
	}
	
	final void triggerPacketDequeue ()
	{
		this.enqueueHandler (new PacketDequeueHandler ());
	}
	
	final void triggerPacketEnqueue (final Session session, final Message message)
	{
		this.enqueueHandler (new PacketEnqueueHandler (session, message));
	}
	
	final void triggerSessionContinueDispatch (final Session session)
	{
		if (session.dispatchContinued.get () == Boolean.FALSE) {
			session.dispatchContinued.set (Boolean.TRUE);
			session.idle.release ();
			this.scheduleDispatcher (session);
		}
	}
	
	private final void enqueueDispatcher (final Dispatcher dispatcher)
	{
		final Session session = dispatcher.session;
		session.dispatchers.add (dispatcher);
		this.scheduleDispatcher (session);
	}
	
	private final void enqueueHandler (final Handler handler)
	{
		this.handlers.add (handler);
		this.scheduleHandler ();
	}
	
	private final void scheduleDispatcher (final Session session)
	{
		if (!session.dispatchers.isEmpty () && session.idle.tryAcquire ()) {
			final Dispatcher dispatcher = session.dispatchers.poll ();
			if (dispatcher != null)
				try {
					session.executor.get ().execute (dispatcher);
				} catch (final Error exception) {
					this.exceptions.traceDeferredException (exception, "error encountered while scheduling dispatcher; rethrowing!");
					session.idle.release ();
					throw (exception);
				}
			else
				session.idle.release ();
		}
	}
	
	private final void scheduleHandler ()
	{
		if (!this.handlers.isEmpty () && this.idle.tryAcquire ()) {
			final Handler handler = this.handlers.poll ();
			if (handler != null)
				try {
					this.executor.execute (handler);
				} catch (final Error exception) {
					this.exceptions.traceDeferredException (exception, "error encountered while scheduling handler; rethrowing!");
					this.idle.release ();
					throw (exception);
				}
			else
				this.idle.release ();
		}
	}
	
	public static final ZeroMqChannel create (final String self, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new ZeroMqChannel (self, threading, exceptions));
	}
	
	final TranscriptExceptionTracer exceptions;
	final ExecutorService executor;
	final ConcurrentLinkedQueue<Handler> handlers;
	final Semaphore idle;
	final String selfIdentifier;
	final ZeroMqChannelSocket socket;
	final State state;
	final ThreadingContext threading;
	final Transcript transcript;
	
	private static final class Acceptor
			extends Object
	{
		Acceptor (final String key, final String selfRoleIdentifier, final String peerRoleIdentifier, final SessionSpecification specification, final SessionCallbacks callbacks)
		{
			super ();
			this.key = key;
			this.selfRoleIdentifier = selfRoleIdentifier;
			this.peerRoleIdentifier = peerRoleIdentifier;
			this.specification = specification;
			this.callbacks = callbacks;
		}
		
		final SessionCallbacks callbacks;
		final String key;
		final String peerRoleIdentifier;
		final String selfRoleIdentifier;
		final SessionSpecification specification;
	}
	
	private static final class Coder
			extends Object
	{
		Coder (final String key, final String selfRoleIdentifier, final String peerRoleIdentifier, final String messageIdentifier, final MessageType messageType, final MessageSpecification specification, final PayloadCoder coder)
		{
			super ();
			this.key = key;
			this.selfRoleIdentifier = selfRoleIdentifier;
			this.peerRoleIdentifier = peerRoleIdentifier;
			this.messageIdentifier = messageIdentifier;
			this.messageType = messageType;
			this.specification = specification;
			this.coder = coder;
		}
		
		final PayloadCoder coder;
		final String key;
		final String messageIdentifier;
		final MessageType messageType;
		final String peerRoleIdentifier;
		final String selfRoleIdentifier;
		final MessageSpecification specification;
	}
	
	private abstract class Dispatcher
			extends Runnable
	{
		Dispatcher (final Session session)
		{
			super ();
			this.session = session;
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.executeDispatcher (this);
		}
		
		abstract void dispatch ();
		
		final Session session;
	}
	
	private abstract class Handler
			extends Runnable
	{
		Handler ()
		{
			super ();
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.executeHandler (this);
		}
		
		abstract void handle ();
	}
	
	private final class PacketDequeueHandler
			extends Handler
	{
		PacketDequeueHandler ()
		{
			super ();
		}
		
		@Override
		final void handle ()
		{
			ZeroMqChannel.this.handlePacketDequeue ();
		}
	}
	
	private final class PacketDequeueTrigger
			extends Trigger
	{
		PacketDequeueTrigger ()
		{
			super ();
		}
		
		@Override
		final void trigger ()
		{
			ZeroMqChannel.this.triggerPacketDequeue ();
		}
	}
	
	private final class PacketEnqueueHandler
			extends Handler
	{
		PacketEnqueueHandler (final Session session, final Message message)
		{
			super ();
			this.session = session;
			this.message = message;
		}
		
		@Override
		final void handle ()
		{
			ZeroMqChannel.this.handlePacketEnqueue (this.session, this.message);
		}
		
		final Message message;
		final Session session;
	}
	
	private abstract class Runnable
			extends Object
			implements
				java.lang.Runnable
	{
		Runnable ()
		{
			super ();
		}
	}
	
	private final class Session
			extends Object
			implements
				eu.mosaic_cloud.interoperability.core.Session
	{
		Session (final String sessionIdentifier, final String selfRoleIdentifier, final String peerRoleIdentifier, final String peerIdentifier, final SessionSpecification specification, final SessionCallbacks callbacks, final Executor executor)
		{
			super ();
			this.sessionIdentifier = sessionIdentifier;
			this.selfRoleIdentifier = selfRoleIdentifier;
			this.peerRoleIdentifier = peerRoleIdentifier;
			this.peerIdentifier = peerIdentifier;
			this.specification = specification;
			this.dispatchers = new ConcurrentLinkedQueue<ZeroMqChannel.Dispatcher> ();
			this.idle = new Semaphore (1);
			this.dispatchContinued = new ThreadLocal<Boolean> ();
			this.callbacks = Atomics.newReference (callbacks);
			this.executor = Atomics.newReference (executor);
		}
		
		@Override
		public final void continueDispatch ()
		{
			ZeroMqChannel.this.triggerSessionContinueDispatch (this);
		}
		
		@Override
		public final void send (final Message message)
		{
			Preconditions.checkNotNull (message);
			ZeroMqChannel.this.triggerPacketEnqueue (this, message);
		}
		
		@Override
		public final void setCallbacks (final SessionCallbacks callbacks)
		{
			Preconditions.checkNotNull (callbacks);
			this.callbacks.set (callbacks);
		}
		
		@Override
		public final void setExecutor (final Executor executor)
		{
			Preconditions.checkNotNull (executor);
			this.executor.set (executor);
		}
		
		final AtomicReference<SessionCallbacks> callbacks;
		final ThreadLocal<Boolean> dispatchContinued;
		final ConcurrentLinkedQueue<Dispatcher> dispatchers;
		final AtomicReference<Executor> executor;
		final Semaphore idle;
		final String peerIdentifier;
		final String peerRoleIdentifier;
		final String selfRoleIdentifier;
		final String sessionIdentifier;
		final SessionSpecification specification;
	}
	
	private final class SessionCreatedHandler
			extends Dispatcher
	{
		SessionCreatedHandler (final Session session)
		{
			super (session);
		}
		
		@Override
		final void dispatch ()
		{
			ZeroMqChannel.this.dispatchSessionCreated (this.session);
		}
	}
	
	private final class SessionDestroyedDispatcher
			extends Dispatcher
	{
		SessionDestroyedDispatcher (final Session session)
		{
			super (session);
		}
		
		@Override
		final void dispatch ()
		{
			ZeroMqChannel.this.dispatchSessionDestroyed (this.session);
		}
	}
	
	private final class SessionReceivedDispatcher
			extends Dispatcher
	{
		SessionReceivedDispatcher (final Session session, final Message message)
		{
			super (session);
			this.message = message;
		}
		
		@Override
		final void dispatch ()
		{
			ZeroMqChannel.this.dispatchSessionReceived (this.session, this.message);
		}
		
		final Message message;
	}
	
	private final class State
			extends Object
	{
		State ()
		{
			super ();
			this.monitor = Monitor.create (this);
			this.sessions = new HashMap<String, Session> ();
			this.acceptors = new HashMap<String, Acceptor> ();
			this.coders = new HashMap<String, Coder> ();
		}
		
		final HashMap<String, Acceptor> acceptors;
		final HashMap<String, Coder> coders;
		final Monitor monitor;
		final HashMap<String, Session> sessions;
	}
	
	private abstract class Trigger
			extends Runnable
	{
		Trigger ()
		{
			super ();
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.executeTrigger (this);
		}
		
		abstract void trigger ();
	}
}
