
package eu.mosaic_cloud.interoperability.core;


public interface MessageSpecification
		extends
			Specification
{
	public abstract MessageCoder getCoder ();
	
	public abstract String getIdentifier ();
	
	public abstract MessageType getType ();
}
