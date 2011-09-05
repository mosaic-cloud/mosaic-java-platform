package mosaic.driver;

/**
 * Enums of component callbacks for resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum DriverCallbackType {
	AMQP("mosaic.driver.queue.amqp.AmqpDriverComponentCallbacks"), KV(
			"mosaic.driver.kvstore.KVDriverComponentCallbacks");

	private final String callbackClass;

	DriverCallbackType(String canonicalClassName) {
		this.callbackClass = canonicalClassName;
	}

	public String getCallbackClass() {
		return this.callbackClass;
	}
}
