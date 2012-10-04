
package eu.mosaic_cloud.platform.core.utils;


public class MessageEnvelope
{
	public EncodingMetadata getEncodingMetadata ()
	{
		return this.encodingMetadata;
	}
	
	public void setEncodingMetadata (final EncodingMetadata encodingMetadata)
	{
		this.encodingMetadata = encodingMetadata;
	}
	
	private EncodingMetadata encodingMetadata;
}
