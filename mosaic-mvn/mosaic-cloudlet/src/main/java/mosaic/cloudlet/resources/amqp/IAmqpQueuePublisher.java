package mosaic.cloudlet.resources.amqp;

/**
 * Interface for registering and using for an AMQP resource as a publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this accessor
 * @param <D>
 *            the type of the published data
 */
public interface IAmqpQueuePublisher<S, D> extends IAmqpQueueAccessor<S> {
	/**
	 * Publishes a message to a queue.
	 * 
	 * @param data
	 *            the data to publish
	 * @param token
	 *            extra info specific to the published data
	 */
	void publish(D data, Object token);
}
