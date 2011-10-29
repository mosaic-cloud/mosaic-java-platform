
package eu.mosaic_cloud.interoperability.examples.kv;


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
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	public final String identifier;
}
