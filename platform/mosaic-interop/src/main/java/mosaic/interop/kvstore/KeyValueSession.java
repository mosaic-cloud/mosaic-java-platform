package mosaic.interop.kvstore;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;

/**
 * Defines the Key-Value session: the messages that can be exchanged and the
 * roles of the participants.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum KeyValueSession implements SessionSpecification {

	CONNECTOR(KeyValueRole.CONNECTOR, KeyValueRole.DRIVER), DRIVER(
			KeyValueRole.DRIVER, KeyValueRole.CONNECTOR);

	public final KeyValueRole selfRole;
	public final KeyValueRole peerRole;
	public final List<KeyValueMessage> messages;

	private KeyValueSession(KeyValueRole selfRole, KeyValueRole peerRole) {
		this.selfRole = selfRole;
		this.peerRole = peerRole;
		List<KeyValueMessage> messages = new LinkedList<KeyValueMessage>();
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

}
