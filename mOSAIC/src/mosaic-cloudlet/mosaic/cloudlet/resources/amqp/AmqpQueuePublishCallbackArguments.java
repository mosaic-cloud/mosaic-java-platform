package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;

/**
 * The arguments of the cloudlet callback methods for the publish request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the state of the cloudlet
 * @param <D>
 *            the type of the published data
 */
public class AmqpQueuePublishCallbackArguments<S, D> extends
		CallbackArguments<S> {
	private AmqpQueuePublishMessage<D> message;

	/**
	 * Creates a new callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param message
	 *            information about the publish request
	 */
	public AmqpQueuePublishCallbackArguments(ICloudletController<S> cloudlet,
			AmqpQueuePublishMessage<D> message) {
		super(cloudlet);
		this.message = message;
	}

	/**
	 * Returns information about the publish request.
	 * 
	 * @return information about the publish request
	 */
	public AmqpQueuePublishMessage<D> getMessage() {
		return message;
	}

}
