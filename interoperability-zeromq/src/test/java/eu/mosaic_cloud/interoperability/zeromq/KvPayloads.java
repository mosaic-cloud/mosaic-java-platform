
package eu.mosaic_cloud.interoperability.zeromq;


import java.io.Serializable;


public interface KvPayloads
{
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
