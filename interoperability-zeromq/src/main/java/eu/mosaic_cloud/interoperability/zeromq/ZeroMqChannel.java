
package eu.mosaic_cloud.interoperability.zeromq;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.MessageCoder;
import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.zeromq.ZeroMqConnection.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ZeroMqChannel
		extends Object
		implements
			Channel
{
	public ZeroMqChannel (final String self)
	{
		super ();
		Preconditions.checkNotNull (self);
		this.monitor = new Object ();
		synchronized (this.monitor) {
			this.logger = LoggerFactory.getLogger (this.getClass ());
			this.selfIdentifier = self;
			this.sessions = new HashMap<String, Session> ();
			this.acceptors = new HashMap<String, Acceptor> ();
			this.coders = new HashMap<String, Coder> ();
			this.executor = Executors.newSingleThreadExecutor (new ThreadFactory () {
				@Override
				public Thread newThread (final Runnable runnable)
				{
					final Thread thread = Executors.defaultThreadFactory ().newThread (runnable);
					thread.setDaemon (true);
					return (thread);
				}
			});
			this.connection = new ZeroMqConnection (self, new DequeueTrigger ());
		}
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
		synchronized (this.monitor) {
			if (this.acceptors.containsKey (acceptorKey)) {
				this.logger.error ("error encountered while registering acceptor: already registered; throwing!");
				throw (new IllegalStateException ());
			}
			this.logger.trace ("registering acceptor: `{}` -> {}...", acceptor.key, acceptor.callbacks);
			this.acceptors.put (acceptorKey, acceptor);
		}
	}
	
	public final void accept (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		synchronized (this.monitor) {
			this.connection.accept (endpoint);
		}
	}
	
	public final void connect (final String endpoint)
	{
		Preconditions.checkNotNull (endpoint);
		synchronized (this.monitor) {
			this.connection.connect (endpoint);
		}
	}
	
	@Override
	public final void create (final String peer, final SessionSpecification specification, final Message message, final SessionCallbacks callbacks)
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
		synchronized (this.monitor) {
			final Session session = new Session (sessionIdentifier, selfRoleIdentifier, peerRoleIdentifier, peer, specification, callbacks);
			this.sessions.put (sessionIdentifier, session);
			this.executor.execute (new SessionCreatedHandler (session));
			this.executor.execute (new SessionSendHandler (session, message));
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
			final MessageCoder messageCoder = messageSpecification.getCoder ();
			Preconditions.checkNotNull (messageIdentifier);
			Preconditions.checkNotNull (messageType);
			final String coderKey = selfRoleIdentifier + "//" + peerRoleIdentifier + "//" + messageIdentifier;
			final Coder coder = new Coder (coderKey, selfRoleIdentifier, peerRoleIdentifier, messageIdentifier, messageType, messageSpecification, messageCoder);
			coders.add (coder);
		}
		synchronized (this.monitor) {
			for (final Coder coder : coders)
				if (this.coders.containsKey (coder.key)) {
					this.logger.error ("error encountered while registering coder: already registered; throwing!");
					throw (new IllegalStateException ());
				}
			for (final Coder coder : coders) {
				this.logger.trace ("registering coder: `{}` -> {}...", coder.key, coder.coder);
				this.coders.put (coder.key, coder);
			}
		}
	}
	
	public final void terminate ()
	{
		synchronized (this.monitor) {
			this.connection.terminate ();
			this.executor.shutdown ();
		}
		try {
			this.executor.awaitTermination (1000, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException exception) {
			return;
		}
	}
	
	private final void handleDequeue ()
	{
		final Packet packet = this.connection.dequeue ();
		if (packet == null)
			throw (new IllegalStateException ());
		final String sessionIdentifier;
		final String selfRoleIdentifier;
		final String peerRoleIdentifier;
		final String messageIdentifier;
		try {
			final DataInputStream stream = new DataInputStream (new ByteArrayInputStream (packet.header));
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
				this.logger.error ("error encountered while decoding packet: header trailing garbage; ignoring!");
				return;
			}
			stream.close ();
		} catch (final IOException exception) {
			this.logger.error ("error encountered while decoding packet; ignoring!", exception);
			return;
		}
		final String acceptorKey = selfRoleIdentifier + "//" + peerRoleIdentifier;
		final String coderKey = selfRoleIdentifier + "//" + peerRoleIdentifier + "//" + messageIdentifier;
		final Session existingSession;
		final Acceptor acceptor;
		final Coder coder;
		synchronized (this.monitor) {
			existingSession = this.sessions.get (sessionIdentifier);
			acceptor = this.acceptors.get (acceptorKey);
			coder = this.coders.get (coderKey);
		}
		if (coder == null) {
			this.logger.error ("error encountered while decoding packet: missing coder; ignoring!");
			return;
		}
		if ((coder.coder == null) && (packet.payload != null)) {
			this.logger.error ("error encountered while decoding packet: missing coder, but existing payload; ignoring!");
			return;
		}
		if ((coder.coder != null) && (packet.payload == null)) {
			this.logger.error ("error encountered while decoding packet: existing coder, but missing payload; ignoring!");
			return;
		}
		final Object payload;
		if (packet.payload != null)
			try {
				payload = coder.coder.decodeMessage (packet.payload);
			} catch (final Throwable exception) {
				this.logger.error ("error encountered while decoding packet: coder failed; ignoring!", exception);
				return;
			}
		else
			payload = null;
		final Session session;
		if (existingSession == null) {
			if (acceptor == null) {
				this.logger.error ("error encountered while initiating session: mismatched roles; ignoring!");
				return;
			}
			if (coder.messageType != MessageType.Initiation) {
				this.logger.error ("error encountered while initiating session: mismatched message type; ignoring!");
				return;
			}
			session = new Session (sessionIdentifier, selfRoleIdentifier, peerRoleIdentifier, packet.peer, acceptor.specification, acceptor.callbacks);
			synchronized (this.monitor) {
				assert (!this.sessions.containsKey (sessionIdentifier));
				this.sessions.put (sessionIdentifier, session);
				this.executor.execute (new SessionCreatedHandler (session));
			}
		} else
			session = existingSession;
		final Message message = new Message (coder.specification, payload);
		synchronized (this.monitor) {
			this.executor.execute (new SessionReceivedHandler (session, message));
			if (coder.messageType == MessageType.Termination)
				this.executor.execute (new SessionDestroyedHandler (session));
		}
	}
	
	private final void handleSessionCreated (final Session session)
	{
		try {
			session.callbacks.created (session);
		} catch (final Error exception) {
			this.logger.error ("error encountered while executing session callbacks; ignoring!");
		}
	}
	
	private final void handleSessionDestroyed (final Session session)
	{
		try {
			session.callbacks.destroyed (session);
		} catch (final Error exception) {
			this.logger.error ("error encountered while executing session callbacks; ignoring!");
		}
	}
	
	private final void handleSessionReceived (final Session session, final Message message)
	{
		try {
			session.callbacks.received (session, message);
		} catch (final Error exception) {
			this.logger.error ("error encountered while executing session callbacks; ignoring!");
		}
	}
	
	private final void handleSessionSend (final Session session, final Message message)
	{
		final String messageIdentifier;
		try {
			messageIdentifier = message.specification.getIdentifier ();
		} catch (final Error exception) {
			this.logger.error ("error encountered while encoding packet; ignoring!", exception);
			return;
		}
		final String coderKey = session.selfRoleIdentifier + "//" + session.peerRoleIdentifier + "//" + messageIdentifier;
		final Coder coder;
		synchronized (this.monitor) {
			coder = this.coders.get (coderKey);
		}
		if (coder == null) {
			this.logger.error ("error encountered while decoding packet: missing coder; ignoring!");
			return;
		}
		// if (coder.messageType != MessageType.Exchange || coder.messageType != MessageType.Termination) {
		// this.logger.error ("error encountered while initiating session: mismatched message type; ignoring!");
		// return;
		// }
		if ((coder.coder == null) && (message.payload != null)) {
			this.logger.error ("error encountered while encoding packet: missing coder, but existing payload; ignoring!");
			return;
		}
		if ((coder.coder != null) && (message.payload == null)) {
			this.logger.error ("error encountered while encoding packet: existing coder, but missing payload; ignoring!");
			return;
		}
		final byte[] payload;
		if (message.payload != null)
			try {
				payload = coder.coder.encodeMessage (message.payload);
			} catch (final Throwable exception) {
				this.logger.error ("error encountered while encoding packet: coder failed; ignoring!", exception);
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
			this.logger.error ("error encountered while encoding packet; ignoring!", exception);
			return;
		}
		final Packet packet = new Packet (session.peerIdentifier, header, payload);
		if (!this.connection.enqueue (packet, 1000))
			throw (new IllegalStateException ());
		if (coder.messageType == MessageType.Termination)
			synchronized (this.monitor) {
				this.executor.execute (new SessionDestroyedHandler (session));
			}
	}
	
	private final void triggerDequeue ()
	{
		synchronized (this.monitor) {
			this.executor.execute (new DequeueHandler ());
		}
	}
	
	private final void triggerSessionSend (final Session session, final Message message)
	{
		synchronized (this.monitor) {
			this.executor.execute (new SessionSendHandler (session, message));
		}
	}
	
	private final HashMap<String, Acceptor> acceptors;
	private final HashMap<String, Coder> coders;
	private final ZeroMqConnection connection;
	private final ExecutorService executor;
	private final Logger logger;
	private final Object monitor;
	private final String selfIdentifier;
	private final HashMap<String, Session> sessions;
	
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
		Coder (final String key, final String selfRoleIdentifier, final String peerRoleIdentifier, final String messageIdentifier, final MessageType messageType, final MessageSpecification specification, final MessageCoder coder)
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
		
		final MessageCoder coder;
		final String key;
		final String messageIdentifier;
		final MessageType messageType;
		final String peerRoleIdentifier;
		final String selfRoleIdentifier;
		final MessageSpecification specification;
	}
	
	private final class DequeueHandler
			extends Object
			implements
				Runnable
	{
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.handleDequeue ();
		}
	}
	
	private final class DequeueTrigger
			extends Object
			implements
				Runnable
	{
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.triggerDequeue ();
		}
	}
	
	private final class Session
			extends Object
			implements
				eu.mosaic_cloud.interoperability.core.Session
	{
		Session (final String sessionIdentifier, final String selfRoleIdentifier, final String peerRoleIdentifier, final String peerIdentifier, final SessionSpecification specification, final SessionCallbacks callbacks)
		{
			super ();
			this.sessionIdentifier = sessionIdentifier;
			this.selfRoleIdentifier = selfRoleIdentifier;
			this.peerRoleIdentifier = peerRoleIdentifier;
			this.peerIdentifier = peerIdentifier;
			this.specification = specification;
			this.callbacks = callbacks;
		}
		
		@Override
		public void send (final Message message)
		{
			Preconditions.checkNotNull (message);
			ZeroMqChannel.this.triggerSessionSend (this, message);
		}
		
		final SessionCallbacks callbacks;
		final String peerIdentifier;
		final String peerRoleIdentifier;
		final String selfRoleIdentifier;
		final String sessionIdentifier;
		final SessionSpecification specification;
	}
	
	private final class SessionCreatedHandler
			extends Object
			implements
				Runnable
	{
		SessionCreatedHandler (final Session session)
		{
			super ();
			this.session = session;
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.handleSessionCreated (this.session);
		}
		
		final Session session;
	}
	
	private final class SessionDestroyedHandler
			extends Object
			implements
				Runnable
	{
		SessionDestroyedHandler (final Session session)
		{
			super ();
			this.session = session;
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.handleSessionDestroyed (this.session);
		}
		
		final Session session;
	}
	
	private final class SessionReceivedHandler
			extends Object
			implements
				Runnable
	{
		SessionReceivedHandler (final Session session, final Message message)
		{
			super ();
			this.session = session;
			this.message = message;
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.handleSessionReceived (this.session, this.message);
		}
		
		final Message message;
		final Session session;
	}
	
	private final class SessionSendHandler
			extends Object
			implements
				Runnable
	{
		SessionSendHandler (final Session session, final Message message)
		{
			super ();
			this.session = session;
			this.message = message;
		}
		
		@Override
		public final void run ()
		{
			ZeroMqChannel.this.handleSessionSend (this.session, this.message);
		}
		
		final Message message;
		final Session session;
	}
}
