
package eu.mosaic_cloud.platform.interop.common.kv;


public class KeyValueMessage
{
	public KeyValueMessage (final String key, final byte[] data, final String contentType)
	{
		this (key, data, null, contentType);
	}
	
	public KeyValueMessage (final String key, final byte[] data, final String contentEncoding, final String contentType)
	{
		super ();
		this.key = key;
		this.data = data;
		this.contentEncoding = contentEncoding;
		this.contentType = contentType;
	}
	
	public String getContentEncoding ()
	{
		return this.contentEncoding;
	}
	
	public String getContentType ()
	{
		return this.contentType;
	}
	
	public byte[] getData ()
	{
		return this.data;
	}
	
	public String getKey ()
	{
		return this.key;
	}
	
	@Override
	public String toString ()
	{
		return this.key + " " + this.contentType + "(" + this.contentEncoding + ")";
	}
	
	final String key;
	final private String contentEncoding;
	final private String contentType;
	final private byte[] data;
}
