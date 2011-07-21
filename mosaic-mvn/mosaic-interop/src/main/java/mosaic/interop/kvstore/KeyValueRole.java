package mosaic.interop.kvstore;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

/**
 * Enum of the possible role of the participants in an Key-Value session.
 * 
 * @author Georgiana Macariu
 * 
 */
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
