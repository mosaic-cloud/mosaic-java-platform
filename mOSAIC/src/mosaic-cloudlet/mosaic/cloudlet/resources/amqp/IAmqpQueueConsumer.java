package mosaic.cloudlet.resources.amqp;


/**
 * Interface for registering and using for an AMQP resource as a consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this accessor
 * @param <D>
 *            the type of the consumed data
 */
public interface IAmqpQueueConsumer<S, D> extends IAmqpQueueAccessor<S> {
	/**
	 * Acknowledges a message.
	 * 
	 * @param message
	 *            the message to acknowledge
	 */
	public void acknowledge(AmqpQueueConsumeMessage<D> message);
}
