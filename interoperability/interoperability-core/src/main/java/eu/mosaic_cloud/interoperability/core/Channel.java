
package eu.mosaic_cloud.interoperability.core;


public interface Channel
{
	public abstract void accept (final SessionSpecification specification, final SessionCallbacks callbacks);
	
	public abstract void connect (final String peer, final SessionSpecification specification, final Message message, final SessionCallbacks callbacks);
	
	public abstract void register (final SessionSpecification specification);
}
