
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
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannelSocket.Packet;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.transcript.core.Transcript;


public final class ZeroMqChannel
		extends Object
		implements
			Channel
{
	public ZeroMqChannel (final String self)
	{
		super ();
		Preconditions.checkNotNull (self);
		this.transcript = Transcript.create (this);
		this.selfIdentifier = self;
		this.state = new State ();
		this.handlers = new ConcurrentLinkedQueue<ZeroMqChannel.Handler> ();
		this.idle = new Semaphore (1);
		this.executor = Executors.newCachedThreadPool (new ExecutorThreadFactory ());
		this.socket = new ZeroMqChannelSocket (this.selfIdentifier, new PacketDequeueTrigger ());
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
			for (final Coder coder : coders)
				if (this.state.coders.containsKey (coder.key)) {
					this.transcript.traceError ("error encountered while registering coder: already registered; throwing!");
					throw (new IllegalStateException ());
				}
			for (final Coder coder : coders) {
				this.transcript.traceError ("registering coder: `%s` -> %s...", coder.key, coder.coder);
				this.state.coders.put (coder.key, coder);
			}
		}
	}
	
	public final boolean terminate (final long timeout)
			throws InterruptedException
	{
		synchronized (this.state.monitor) {
			this.socket.terminate ();
			this.executor.shutdown ();
		}
		return (this.executor.awaitTermination (timeout, TimeUnit.MILLISECONDS));
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
	
	final void executeHandler (final Handler handler)
	{
		try {
			handler.handle ();
		} catch (final Error exception) {
			this.transcript.traceIgnoredException (exception, "error encountered while executing handler; ignoring!");
		}
		this.idle.release ();
		this.scheduleHandler ();
	}
	
	final void executeTrigger (final Trigger trigger)
	{
		try {
			trigger.trigger ();
		} catch (final Error exception) {
			this.transcript.traceIgnoredException (exception, "error encountered while executing trigger; ignoring!");
		}
	}
	
	final void handlePacketDequeue ()
	{
		synchronized (this.state.monitor) {
			final Packet packet = this.socket.dequeue ();
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
				this.transcript.traceIgnoredException (exception, "error encountered while decoding packet; ignoring!");
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
					this.transcript.traceIgnoredException (exception, "error encountered while decoding packet: coder failed; ignoring!");
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
	
	private final void executeDispatcher (final Dispatcher dispatcher)
	{
		final Session session = dispatcher.session;
		session.dispatchContinued.set (Boolean.FALSE);
		try {
			dispatcher.dispatch ();
		} catch (final Error exception) {
			this.transcript.traceIgnoredException (exception, "error encountered while executing dispatcher; ignoring!");
		}
		if (session.dispatchContinued.get () == Boolean.FALSE) {
			session.idle.release ();
			this.scheduleDispatcher (session);
		}
		session.dispatchContinued.set (null);
	}
	
	private final void handlePacketEnqueue (final Session session, final Message message)
	{
		synchronized (this.state.monitor) {
			final String messageIdentifier;
			try {
				messageIdentifier = message.specification.getIdentifier ();
			} catch (final Error exception) {
				this.transcript.traceIgnoredException (exception, "error encountered while encoding packet; ignoring!");
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
					this.transcript.traceIgnoredException (exception, "error encountered while encoding packet: coder failed; ignoring!");
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
				this.transcript.traceIgnoredException (exception, "error encountered while encoding packet; ignoring!");
				return;
			}
			final Packet packet = new Packet (session.peerIdentifier, ByteBuffer.wrap (header), payload);
			if (!this.socket.enqueue (packet, 1000))
				throw (new IllegalStateException ());
			if (coder.messageType == MessageType.Termination)
				this.enqueueDispatcher (new SessionDestroyedDispatcher (session));
		}
	}
	
	private final void scheduleDispatcher (final Session session)
	{
		if (!session.dispatchers.isEmpty () && session.idle.tryAcquire ())
			try {
				session.executor.get ().execute (session.dispatchers.poll ());
			} catch (final Error exception) {
				this.transcript.traceRethrownException (exception, "error encountered while scheduling dispatcher; rethrowing!");
				session.idle.release ();
				throw (exception);
			}
	}
	
	private final void scheduleHandler ()
	{
		if ((this.handlers.size () > 0) && this.idle.tryAcquire ())
			try {
				this.executor.execute (this.handlers.poll ());
			} catch (final Error exception) {
				this.transcript.traceRethrownException (exception, "error encountered while scheduling handler; rethrowing!");
				this.idle.release ();
				throw (exception);
			}
	}
	
	private final ExecutorService executor;
	private final ConcurrentLinkedQueue<Handler> handlers;
	private final Semaphore idle;
	private final String selfIdentifier;
	private final ZeroMqChannelSocket socket;
	private final State state;
	private final Transcript transcript;
	
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
	
	private final class ExecutorThreadFactory
			extends Object
			implements
				ThreadFactory
	{
		ExecutorThreadFactory ()
		{
			super ();
		}
		
		@Override
		public final Thread newThread (final java.lang.Runnable runnable)
		{
			final Thread thread = Executors.defaultThreadFactory ().newThread (runnable);
			thread.setName (String.format ("%s#%08x#%08x", ZeroMqChannel.this.getClass ().getSimpleName (), Integer.valueOf (System.identityHashCode (ZeroMqChannel.this)), Integer.valueOf (System.identityHashCode (thread))));
			thread.setDaemon (true);
			return (thread);
		}
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
		@Override
		final void handle ()
		{
			ZeroMqChannel.this.handlePacketDequeue ();
		}
	}
	
	private final class PacketDequeueTrigger
			extends Trigger
	{
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
			this.callbacks = new AtomicReference<SessionCallbacks> (callbacks);
			this.executor = new AtomicReference<Executor> (executor);
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
