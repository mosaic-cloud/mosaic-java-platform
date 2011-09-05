package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;

/**
 * Interface for AMQP queue consumers. This will be implemented by cloudlets
 * which need to receive messages from a queue.
 * 
 * @author Georgiana Macariu
 * @param <S>
 *            the type of the cloudlet state
 * @param <D>
 *            the type of consumed data
 * 
 */
public interface IAmqpQueueConsumerCallback<S, D> extends
		IAmqpQueueAccessorCallback<S> {
	/**
	 * Handles successful message acknowledge events.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void acknowledgeSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Handles unsuccessful message acknowledge events.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void acknowledgeFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Called when this consumer receives a message. This will deliver the
	 * message
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void consume(S state, AmqpQueueConsumeCallbackArguments<S, D> arguments);
}
