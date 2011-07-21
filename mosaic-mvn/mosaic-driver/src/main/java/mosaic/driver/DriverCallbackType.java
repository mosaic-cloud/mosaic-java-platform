package mosaic.driver;

import mosaic.driver.queue.amqp.AmqpDriverComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCallbacks;

/**
 * Enums of component callbacks for resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum DriverCallbackType {
	AMQP(AmqpDriverComponentCallbacks.class);

	private final Class<? extends ComponentCallbacks> callbackClass;

	DriverCallbackType(Class<? extends ComponentCallbacks> canonicalClassName) {
		this.callbackClass = canonicalClassName;
	}

	public Class<? extends ComponentCallbacks> getCallbackClass() {
		return this.callbackClass;
	}
}
