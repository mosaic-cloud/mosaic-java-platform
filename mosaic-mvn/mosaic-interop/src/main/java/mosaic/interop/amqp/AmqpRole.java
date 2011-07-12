package mosaic.interop.amqp;

import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

public enum AmqpRole implements RoleSpecification {
	CONNECTOR(), DRIVER();

	public final String identifier;

	private AmqpRole() {
		this.identifier = Identifiers.generate(this);
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

}
