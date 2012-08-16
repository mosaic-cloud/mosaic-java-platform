package eu.mosaic_cloud.platform.interop.specs.dfs;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

/**
 * Defines the Dfs session: the messages that can be exchanged and the
 * roles of the participants.
 */
public enum DFSSession
		implements
			SessionSpecification
{
	CONNECTOR (DFSRole.CONNECTOR, DFSRole.DRIVER),
	DRIVER (DFSRole.DRIVER, DFSRole.CONNECTOR);
	private DFSSession (final DFSRole selfRole, final DFSRole peerRole)
	{
		this.selfRole = selfRole;
		this.peerRole = peerRole;
		final List<MessageSpecification> messages = new LinkedList<MessageSpecification> ();
		for (final DFSMessage message : DFSMessage.values ()) {
			messages.add (message);
		}
		for (final DFSHandlerMessage message : DFSHandlerMessage.values ()) {
			messages.add (message);
		}
		this.messages = Collections.unmodifiableList (messages);
	}
	
	@Override
	public Iterable<? extends MessageSpecification> getMessages ()
	{
		return this.messages;
	}
	
	@Override
	public RoleSpecification getPeerRole ()
	{
		return this.peerRole;
	}
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	@Override
	public RoleSpecification getSelfRole ()
	{
		return this.selfRole;
	}
	
	public final List<MessageSpecification> messages;
	public final DFSRole peerRole;
	public final DFSRole selfRole;
}
