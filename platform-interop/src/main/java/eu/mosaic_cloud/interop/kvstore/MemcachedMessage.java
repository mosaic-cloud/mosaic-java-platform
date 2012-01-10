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
package eu.mosaic_cloud.interop.kvstore;


import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interop.idl.DefaultPBPayloadCoder;
import eu.mosaic_cloud.interop.idl.kvstore.MemcachedPayloads;
import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

/**
 * Enum containing all possible MEMCACHED connector-driver messages.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedMessage implements MessageSpecification {

	APPEND_REQUEST(MessageType.Exchange, MemcachedPayloads.AppendRequest.class), PREPEND_REQUEST(
			MessageType.Exchange, MemcachedPayloads.PrependRequest.class), ADD_REQUEST(
			MessageType.Exchange, MemcachedPayloads.AddRequest.class), REPLACE_REQUEST(
			MessageType.Exchange, MemcachedPayloads.ReplaceRequest.class), CAS_REQUEST(
			MessageType.Exchange, MemcachedPayloads.CasRequest.class);

	public PayloadCoder coder = null;
	public final String identifier;
	public final MessageType type;

	/**
	 * Creates a new Memcached message.
	 * 
	 * @param type
	 *            the type of the message (initiation, exchange or termination)
	 * @param clasz
	 *            the class containing the payload of the message
	 */
	MemcachedMessage(MessageType type, Class<? extends GeneratedMessage> clasz) {
		this.identifier = Identifiers.generate(this);
		this.type = type;
		if (clasz != null) {
			this.coder = new DefaultPBPayloadCoder(clasz, false);
		}
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public PayloadCoder getPayloadCoder() {
		return this.coder;
	}

	@Override
	public MessageType getType() {
		return this.type;
	}

	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
}
