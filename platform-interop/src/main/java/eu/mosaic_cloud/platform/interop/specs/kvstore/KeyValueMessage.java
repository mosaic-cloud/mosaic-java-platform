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


import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.tools.DefaultPBPayloadCoder;

import com.google.protobuf.GeneratedMessage;


/**
 * Enum containing all possible AMQP connector-driver messages.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum KeyValueMessage
		implements
			MessageSpecification
{
	ABORTED (MessageType.Termination, IdlCommon.AbortRequest.class),
	ACCESS (MessageType.Initiation, KeyValuePayloads.InitRequest.class),
	DELETE_REQUEST (MessageType.Exchange, KeyValuePayloads.DeleteRequest.class),
	ERROR (MessageType.Exchange, IdlCommon.Error.class),
	GET_REPLY (MessageType.Exchange, KeyValuePayloads.GetReply.class),
	GET_REQUEST (MessageType.Exchange, KeyValuePayloads.GetRequest.class),
	LIST_REPLY (MessageType.Exchange, KeyValuePayloads.ListReply.class),
	LIST_REQUEST (MessageType.Exchange, KeyValuePayloads.ListRequest.class),
	NOK (MessageType.Exchange, IdlCommon.NotOk.class),
	OK (MessageType.Exchange, IdlCommon.Ok.class),
	SET_REQUEST (MessageType.Exchange, KeyValuePayloads.SetRequest.class);
	/**
	 * Creates a new Key-Value message.
	 * 
	 * @param type
	 *            the type of the message (initiation, exchange or termination)
	 * @param clasz
	 *            the class containing the payload of the message
	 */
	KeyValueMessage (final MessageType type, final Class<? extends GeneratedMessage> clasz)
	{
		this.identifier = Identifiers.generate (this);
		this.type = type;
		if (clasz != null) {
			this.coder = new DefaultPBPayloadCoder (clasz, false);
		}
	}
	
	@Override
	public String getIdentifier ()
	{
		return this.identifier;
	}
	
	@Override
	public PayloadCoder getPayloadCoder ()
	{
		return this.coder;
	}
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	@Override
	public MessageType getType ()
	{
		return this.type;
	}
	
	public PayloadCoder coder = null;
	public final String identifier;
	public final MessageType type;
}
