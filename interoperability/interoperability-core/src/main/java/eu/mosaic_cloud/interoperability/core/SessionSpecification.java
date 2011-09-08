
package eu.mosaic_cloud.interoperability.core;


public interface SessionSpecification
		extends
			Specification
{
	public abstract Iterable<? extends MessageSpecification> getMessages ();
	
	public abstract RoleSpecification getPeerRole ();
	
	public abstract RoleSpecification getSelfRole ();
}
