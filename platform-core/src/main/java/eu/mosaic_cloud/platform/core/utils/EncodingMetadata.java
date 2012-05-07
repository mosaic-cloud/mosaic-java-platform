
package eu.mosaic_cloud.platform.core.utils;


public final class EncodingMetadata
{
	public EncodingMetadata (final String contentType, final String contentEncoding)
	{
		super ();
		this.contentType = contentType;
		this.contentEncoding = contentEncoding;
	}
	
	public final String contentEncoding;
	public final String contentType;
	public static final EncodingMetadata NULL = new EncodingMetadata (null, null);
}
