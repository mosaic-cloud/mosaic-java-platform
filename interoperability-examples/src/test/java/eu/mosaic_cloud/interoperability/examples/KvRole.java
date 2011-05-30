
package eu.mosaic_cloud.interoperability.examples;


import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


public enum KvRole
		implements
			RoleSpecification
{
	Client (),
	Server ();
	KvRole ()
	{
		this.identifier = Identifiers.generate (this);
	}
	
	@Override
	public String getIdentifier ()
	{
		return (this.identifier);
	}
	
	public final String identifier;
}
