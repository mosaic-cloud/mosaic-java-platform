package mosaic.driver.queue.amqp;

/**
 * Interface for application callback objects to receive notifications and
 * messages from a queue by subscription. Methods of this interface are invoked
 * inside the driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IAmqpConsumer {
	/**
	 * Handles the Consume OK message.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 */
	void handleConsumeOk(String consumerTag);

	/**
	 * Handles the Cancel OK message.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 */
	void handleCancelOk(String consumerTag);

	/**
	 * Handles the Cancel message. Called when the consumer is cancelled for
	 * reasons other than by a basicCancel.
	 * 
	 * @param consumerTag
	 *            the consumer identifier
	 */
	void handleCancel(String consumerTag);

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
	void handleShutdown(String consumerTag, String signalMessage);
}
