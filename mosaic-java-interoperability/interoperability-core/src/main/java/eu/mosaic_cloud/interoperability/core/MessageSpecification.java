
package eu.mosaic_cloud.interoperability.core;


public interface MessageSpecification
		extends
			Specification
{
	public abstract String getIdentifier ();
	
	public abstract PayloadCoder getPayloadCoder ();
	
	public abstract MessageType getType ();
}
