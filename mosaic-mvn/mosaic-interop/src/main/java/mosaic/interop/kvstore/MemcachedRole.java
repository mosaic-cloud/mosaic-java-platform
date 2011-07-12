package mosaic.interop.kvstore;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

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

}
