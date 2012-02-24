
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;

import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

public interface IAmqpQueuePublisherConnectorFactory extends
        IAmqpQueueConnectorFactory<IAmqpQueueConsumerConnector<?, ?, ?>> {

    <Context, Message, Extra> IAmqpQueuePublisherConnector<Context, Message, Extra> create(
            IConfiguration configuration, Class<Message> messageClass,
            DataEncoder<? super Message> messageEncoder,
            IAmqpQueuePublisherConnectorCallback<Context, Message, Extra> callback,
            Context callbackContext);
}
