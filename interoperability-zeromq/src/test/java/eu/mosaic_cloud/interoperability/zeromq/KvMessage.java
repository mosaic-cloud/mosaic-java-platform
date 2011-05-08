
package eu.mosaic_cloud.interoperability.zeromq;


import java.io.Serializable;

import eu.mosaic_cloud.interoperability.core.MessageCoder;
import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.tools.DefaultJavaSerializationCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


public enum KvMessage
		implements
			MessageSpecification
{
	Aborted (MessageType.Termination, null),
	Access (MessageType.Initiation, null),
	Error (MessageType.Exchange, Error.class),
	GetReply (MessageType.Exchange, GetReply.class),
	GetRequest (MessageType.Exchange, GetRequest.class),
	Ok (MessageType.Exchange, Ok.class),
	PutRequest (MessageType.Exchange, PutRequest.class);
	
	KvMessage (final MessageType type, final Class<? extends Serializable> clasz)
	{
		this.identifier = Identifiers.generate (this);
		this.type = type;
		if (clasz != null)
			this.coder = new DefaultJavaSerializationCoder (clasz, false);
		else
			this.coder = null;
	}
	
	@Override
	public MessageCoder getCoder ()
	{
		return (this.coder);
	}
	
	@Override
	public String getIdentifier ()
	{
		return (this.identifier);
	}
	
	@Override
	public String getName ()
	{
		return (this.name ());
	}
	
	@Override
	public MessageType getType ()
	{
		return (this.type);
	}
	
	public final MessageCoder coder;
	public final String identifier;
	public final MessageType type;
	
	public static final class Error
			implements
				Serializable
	{
		public Error (final long sequence)
		{
			this.sequence = sequence;
		}
		
		public final long sequence;
		
		private static final long serialVersionUID = 1L;
	}
	
	public static final class GetReply
			implements
				Serializable
	{
		public GetReply (final long sequence, final String value)
		{
			this.sequence = sequence;
			this.value = value;
		}
		
		public final long sequence;
		public final String value;
		
		private static final long serialVersionUID = 1L;
	}
	
	public static final class GetRequest
			implements
				Serializable
	{
		public GetRequest (final long sequence, final String key)
		{
			this.sequence = sequence;
			this.key = key;
		}
		
		public final String key;
		public final long sequence;
		
		private static final long serialVersionUID = 1L;
	}
	
	public static final class Ok
			implements
				Serializable
	{
		public Ok (final long sequence)
		{
			this.sequence = sequence;
		}
		
		public final long sequence;
		
		private static final long serialVersionUID = 1L;
	}
	
	public static final class PutRequest
			implements
				Serializable
	{
		public PutRequest (final long sequence, final String key, final String value)
		{
			this.sequence = sequence;
			this.key = key;
			this.value = value;
		}
		
		public final String key;
		public final long sequence;
		public final String value;
		
		private static final long serialVersionUID = 1L;
	}
}
