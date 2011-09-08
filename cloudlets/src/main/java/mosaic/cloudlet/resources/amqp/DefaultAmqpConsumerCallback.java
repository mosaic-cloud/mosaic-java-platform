package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;

/**
 * Default AMQP consumer callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 * @param <D>
 *            the type of consumed data
 */
public class DefaultAmqpConsumerCallback<S, D> extends
		DefaultAmqpAccessorCallback<S> implements
		IAmqpQueueConsumerCallback<S, D> {

	@Override
	public void acknowledgeSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Succeeded", true,
				false);
	}

	@Override
	public void acknowledgeFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Failed", false,
				false);
	}

	@Override
	public void consume(S state,
			AmqpQueueConsumeCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Consume", true, false);
	}

}
