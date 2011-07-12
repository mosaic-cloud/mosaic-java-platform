package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;

/**
 * The arguments of the cloudlet callback methods for the consume request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the state of the cloudlet
 * @param <D>
 *            the type of the consumed data
 */
public class AmqpQueueConsumeCallbackArguments<S, D> extends
		CallbackArguments<S> {
	private AmqpQueueConsumeMessage<D> message;

	/**
	 * Creates a new callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param message
	 *            information about the consume request
	 */
	public AmqpQueueConsumeCallbackArguments(ICloudletController<S> cloudlet,
			AmqpQueueConsumeMessage<D> message) {
		super(cloudlet);
		this.message = message;
	}

	/**
	 * Returns information about the consume request.
	 * 
	 * @return information about the consume request
	 */
	public AmqpQueueConsumeMessage<D> getMessage() {
		return this.message;
	}

}
