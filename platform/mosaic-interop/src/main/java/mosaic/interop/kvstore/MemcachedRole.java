package mosaic.interop.kvstore;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

/**
 * Enum of the possible role of the participants in an Memcached session.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedRole implements RoleSpecification {
	CONNECTOR(), DRIVER();

	public final String identifier;

	private MemcachedRole() {
		this.identifier = Identifiers.generate(this);
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
}
