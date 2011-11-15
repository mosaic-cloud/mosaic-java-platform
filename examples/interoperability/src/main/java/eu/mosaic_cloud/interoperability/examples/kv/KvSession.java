/*
 * #%L
 * interoperability-examples
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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

package eu.mosaic_cloud.interoperability.examples.kv;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


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
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
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
