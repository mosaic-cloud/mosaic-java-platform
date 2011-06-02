package mosaic.cloudlet.resources.amqp;

/**
 * Interface for AMQP queue publishers. This will be implemented by cloudlets
 * which need to send messages to an exchange.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 * @param <D>
 *            the type of published data
 */
public interface IAmqpQueuePublisherCallback<S, D> extends
		IAmqpQueueAccessorCallback<S> {
	/**
	 * Called when the publisher receives confirmation that the message
	 * publishing finished successfully.
	 * 
	 * @param <D>
	 *            the type of the published message
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void publishSucceeded(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments);

	/**
	 * Called when the publisher receives notification that the message
	 * publishing could not be finished with success.
	 * 
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void publishFailed(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments);
}
