/*
 * #%L
 * mosaic-interop
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
