package mosaic.cloudlet.resources.amqp;

import mosaic.connector.queue.AmqpInboundMessage;

/**
 * An object of this class embeds the essential information about a consume
 * request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <D>
 *            the type of the data in the consumed message
 */
public class AmqpQueueConsumeMessage<D extends Object> {
	private final AmqpQueueConsumer<? extends Object, D> consumer;
	private final AmqpInboundMessage message;
	private final D data;

	public AmqpQueueConsumeMessage(
			AmqpQueueConsumer<? extends Object, D> consumer,
			AmqpInboundMessage message, D data) {
		super();
		this.consumer = consumer;
		this.message = message;
		this.data = data;
	}

	/**
	 * Acknowledges the message.
	 */
	public void acknowledge() {
		this.consumer.acknowledge(this);
	}

	/**
	 * Returns the data in the consumed message.
	 * 
	 * @return the data in the consumed message
	 */
	public D getData() {
		return data;
	}

	AmqpInboundMessage getMessage() {
		return message;
	}

	/**
	 * Returns the consumer object.
	 * 
	 * @return the consumer object
	 */
	public AmqpQueueConsumer<? extends Object, D> getConsumer() {
		return consumer;
	}

}
