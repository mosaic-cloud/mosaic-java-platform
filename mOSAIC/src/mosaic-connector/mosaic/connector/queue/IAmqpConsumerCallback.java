package mosaic.connector.queue;

/**
 * Interface for handlers (callbacks) to be called when a queue consumer
 * receives a message. Methods defined in this interface are called by the
 * connector when one of the consume messages is received from the driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IAmqpConsumerCallback {
	/**
	 * Handles the Cancel OK message.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 */
	void handleCancelOk(String consumerTag);

	/**
	 * Handles the Consume OK message.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 */
	void handleConsumeOk(String consumerTag);

	/**
	 * Handles a delivered message.
	 * 
	 * @param message
	 *            the message and all its properties
	 */
	void handleDelivery(AmqpInboundMessage message);

	/**
	 * Handles the shutdown signals.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 * @param signalMessage
	 *            the signal message
	 */
	void handleShutdownSignal(String consumerTag, String message);

}
