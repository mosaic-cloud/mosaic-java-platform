package mosaic.connector.queue.amqp;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a Map between consumer identifiers and consume callbacks.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpCallbacksMap {

	private Map<String, IAmqpConsumerCallback> handlerMap = new HashMap<String, IAmqpConsumerCallback>();

	public AmqpCallbacksMap() {
		super();
	}

	/**
	 * Add callback for the consumer identified with the given identifier. If
	 * another callback has been added previously for the consumer, this
	 * callback will be replaced.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @param callback
	 *            the callback
	 */
	public synchronized void addHandlers(String consumerId,
			IAmqpConsumerCallback callback) {
		this.handlerMap.put(consumerId, callback);
	}

	/**
	 * Removes from the map the callback for a consumer and the actual consumer.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @return the callback
	 */
	public synchronized IAmqpConsumerCallback removeConsumerCallback(
			String consumerId) {
		return this.handlerMap.remove(consumerId);
	}

	/**
	 * Returns the callback for a consumer and the actual consumer.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @return the callback
	 */
	public synchronized IAmqpConsumerCallback getRequestHandlers(
			String consumerId) {
		return this.handlerMap.get(consumerId);
	}
}
