
package eu.mosaic_cloud.interoperability.zeromq;


import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


public enum KvRole
		implements
			RoleSpecification
{
	Client (),
	Server ();
	
	@Override
	public String getIdentifier ()
	{
		return (Identifiers.generate (this));
	}
	
	@Override
	public String getName ()
	{
		return (this.name ());
	}
}
