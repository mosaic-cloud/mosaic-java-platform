
package eu.mosaic_cloud.interoperability.zeromq;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;


public enum KvSession
		implements
			SessionSpecification
{
	Client (KvRole.Client, KvRole.Server),
	Server (KvRole.Server, KvRole.Client);
	
	KvSession (final KvRole selfRole, final KvRole peerRole)
	{
		this.selfRole = selfRole;
		this.peerRole = peerRole;
		final LinkedList<KvMessage> messages = new LinkedList<KvMessage> ();
		for (final KvMessage message : KvMessage.values ())
			messages.add (message);
		this.messages = Collections.unmodifiableList (messages);
	}
	
	@Override
	public Iterable<KvMessage> getMessages ()
	{
		return (this.messages);
	}
	
	@Override
	public RoleSpecification getPeerRole ()
	{
		return (this.peerRole);
	}
	
	@Override
	public RoleSpecification getSelfRole ()
	{
		return (this.selfRole);
	}
	
	public final List<KvMessage> messages;
	public final KvRole peerRole;
	public final KvRole selfRole;
}
