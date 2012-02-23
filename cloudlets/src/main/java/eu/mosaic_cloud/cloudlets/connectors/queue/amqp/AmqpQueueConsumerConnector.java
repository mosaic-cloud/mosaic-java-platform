
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueDeliveryToken;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public class AmqpQueueConsumerConnector<Context, Message, Extra>
		extends BaseAmqpQueueConnector<eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<Message>, IAmqpQueueConsumerConnectorCallback<Context, Message, Extra>, Context>
		implements
			IAmqpQueueConsumerConnector<Context, Message, Extra>
{
	public AmqpQueueConsumerConnector (final ICloudletController<?> cloudlet, final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<Message> connector, final IConfiguration configuration, final IAmqpQueueConsumerConnectorCallback<Context, Message, Extra> callback, final Context context)
	{
		super (cloudlet, connector, configuration, callback, context);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final IAmqpQueueDeliveryToken delivery)
	{
		return this.acknowledge (delivery, null);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final IAmqpQueueDeliveryToken delivery, final Extra extra)
	{
		final CallbackCompletion<Void> completion = this.connector.acknowledge (delivery);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null)
						return AmqpQueueConsumerConnector.this.callback.acknowledgeFailed (AmqpQueueConsumerConnector.this.context, new GenericCallbackCompletionArguments<Context, Extra> (AmqpQueueConsumerConnector.this.cloudlet, completion.getException ()));
					return AmqpQueueConsumerConnector.this.callback.acknowledgeSucceeded (AmqpQueueConsumerConnector.this.context, new GenericCallbackCompletionArguments<Context, Extra> (AmqpQueueConsumerConnector.this.cloudlet, extra));
				}
			});
		}
		return completion;
	}
}
