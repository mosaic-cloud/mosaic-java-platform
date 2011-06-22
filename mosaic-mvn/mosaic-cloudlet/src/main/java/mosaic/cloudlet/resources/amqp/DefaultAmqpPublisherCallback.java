package mosaic.cloudlet.resources.amqp;

/**
 * Default AMQP publisher callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 * @param <D>
 *            the type of published data
 */
public class DefaultAmqpPublisherCallback<S, D> extends
		DefaultAmqpAccessorCallback<S> implements
		IAmqpQueuePublisherCallback<S, D> {

	@Override
	public void publishSucceeded(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Publish Succeeded", true,
				false);

	}

	@Override
	public void publishFailed(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Publish Failed", false, false);

	}

}
