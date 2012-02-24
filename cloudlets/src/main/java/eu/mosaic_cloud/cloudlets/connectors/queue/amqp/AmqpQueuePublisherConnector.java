
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;

import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

public class AmqpQueuePublisherConnector<Context, Message, Extra>
        extends
        BaseAmqpQueueConnector<eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnector<Message>, IAmqpQueuePublisherConnectorCallback<Context, Message, Extra>, Context>
        implements IAmqpQueuePublisherConnector<Context, Message, Extra> {

    public AmqpQueuePublisherConnector(
            final ICloudletController<?> cloudlet,
            final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnector<Message> connector,
            final IConfiguration configuration,
            final IAmqpQueuePublisherConnectorCallback<Context, Message, Extra> callback,
            final Context context) {
        super(cloudlet, connector, configuration, callback, context);
    }

    @Override
    public CallbackCompletion<Void> publish(final Message message) {
        return this.publish(message, null);
    }

    @Override
    public CallbackCompletion<Void> publish(final Message message, final Extra extra) {
        final CallbackCompletion<Void> completion = this.connector.publish(message);
        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(final CallbackCompletion<?> completion_) {
                    assert (completion_ == completion);
                    if (completion.getException() != null) {
                        return AmqpQueuePublisherConnector.this.callback.publishFailed(
                                AmqpQueuePublisherConnector.this.context,
                                new GenericCallbackCompletionArguments<Context, Extra>(
                                        AmqpQueuePublisherConnector.this.cloudlet, completion
                                                .getException()));
                    }
                    return AmqpQueuePublisherConnector.this.callback.publishSucceeded(
                            AmqpQueuePublisherConnector.this.context,
                            new GenericCallbackCompletionArguments<Context, Extra>(
                                    AmqpQueuePublisherConnector.this.cloudlet, extra));
                }
            });
        }
        return completion;
    }
}
