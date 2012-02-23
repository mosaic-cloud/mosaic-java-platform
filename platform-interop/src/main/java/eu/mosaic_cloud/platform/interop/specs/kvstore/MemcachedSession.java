/*
 * #%L
 * mosaic-platform-interop
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
package eu.mosaic_cloud.platform.interop.specs.kvstore;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

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
	public String getQualifiedName() {
		return (Identifiers.generateName(this));
	}
}
