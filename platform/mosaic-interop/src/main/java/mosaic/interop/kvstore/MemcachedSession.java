package mosaic.interop.kvstore;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.tools.Identifiers;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;

/**
 * Defines the Memcached session: the messages that can be exchanged and the
 * roles of the participants. The messages exchanged here can be either of type
 * {@linkplain KeyValueMessage} or {@linkplain MemcachedMessage}.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedSession implements SessionSpecification {

	CONNECTOR(MemcachedRole.CONNECTOR, MemcachedRole.DRIVER), DRIVER(
			MemcachedRole.DRIVER, MemcachedRole.CONNECTOR);

	public final MemcachedRole selfRole;
	public final MemcachedRole peerRole;
	public final List<MessageSpecification> messages;

	private MemcachedSession(MemcachedRole selfRole, MemcachedRole peerRole) {
		this.selfRole = selfRole;
		this.peerRole = peerRole;
		List<MessageSpecification> messages = new LinkedList<MessageSpecification>();
		for (MemcachedMessage message : MemcachedMessage.values()) {
			messages.add(message);
		}
		for (KeyValueMessage message : KeyValueMessage.values()) {
			messages.add(message);
		}
		this.messages = Collections.unmodifiableList(messages);
	}

	@Override
	public Iterable<? extends MessageSpecification> getMessages() {
		return this.messages;
	}

	@Override
	public RoleSpecification getPeerRole() {
		return this.peerRole;
	}

	@Override
	public RoleSpecification getSelfRole() {
		return this.selfRole;
	}

	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
}
