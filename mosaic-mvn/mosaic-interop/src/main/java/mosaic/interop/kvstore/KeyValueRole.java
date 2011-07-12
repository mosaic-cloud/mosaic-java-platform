package mosaic.interop.kvstore;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

public enum KeyValueRole implements RoleSpecification {
	CONNECTOR(), DRIVER();

	public final String identifier;

	private KeyValueRole() {
		this.identifier = Identifiers.generate(this);
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

}
