package mosaic.interop.amqp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;

public enum AmqpSession implements SessionSpecification {

	CONNECTOR(AmqpRole.CONNECTOR, AmqpRole.DRIVER), DRIVER(AmqpRole.DRIVER,
			AmqpRole.CONNECTOR);

	public final AmqpRole selfRole;
	public final AmqpRole peerRole;
	public final List<AmqpMessage> messages;

	private AmqpSession(AmqpRole selfRole, AmqpRole peerRole) {
		this.selfRole = selfRole;
		this.peerRole = peerRole;
		List<AmqpMessage> messages = new LinkedList<AmqpMessage>();
		for (AmqpMessage message : AmqpMessage.values()) {
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
