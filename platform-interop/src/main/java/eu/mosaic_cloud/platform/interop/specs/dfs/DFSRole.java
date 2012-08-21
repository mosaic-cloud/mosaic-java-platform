
package eu.mosaic_cloud.platform.interop.specs.dfs;


import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;


/**
 * Enum of the possible role of the participants in an DFS session.
 * 
 */
public enum DFSRole
		implements
			RoleSpecification
{
	CONNECTOR (),
	DRIVER ();
	private DFSRole ()
	{
		this.identifier = Identifiers.generate (this);
	}
	
	@Override
	public String getIdentifier ()
	{
		return this.identifier;
	}
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	public final String identifier;
}
