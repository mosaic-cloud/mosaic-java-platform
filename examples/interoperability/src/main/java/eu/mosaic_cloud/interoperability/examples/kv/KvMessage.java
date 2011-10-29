
package eu.mosaic_cloud.interoperability.examples.kv;


import java.io.Serializable;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.DefaultJavaSerializationPayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


public enum KvMessage
		implements
			MessageSpecification
{
	Aborted (MessageType.Termination, null),
	Access (MessageType.Initiation, null),
	Error (MessageType.Exchange, Error.class),
	GetReply (MessageType.Exchange, KvPayloads.GetReply.class),
	GetRequest (MessageType.Exchange, KvPayloads.GetRequest.class),
	Ok (MessageType.Exchange, KvPayloads.Ok.class),
	PutRequest (MessageType.Exchange, KvPayloads.PutRequest.class);
	KvMessage (final MessageType type, final Class<? extends Serializable> clasz)
	{
		this.identifier = Identifiers.generate (this);
		this.type = type;
		if (clasz != null)
			this.coder = new DefaultJavaSerializationPayloadCoder (clasz, false);
		else
			this.coder = null;
	}
	
	@Override
	public String getIdentifier ()
	{
		return (this.identifier);
	}
	
	@Override
	public PayloadCoder getPayloadCoder ()
	{
		return (this.coder);
	}
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	@Override
	public MessageType getType ()
	{
		return (this.type);
	}
	
	public final PayloadCoder coder;
	public final String identifier;
	public final MessageType type;
}
