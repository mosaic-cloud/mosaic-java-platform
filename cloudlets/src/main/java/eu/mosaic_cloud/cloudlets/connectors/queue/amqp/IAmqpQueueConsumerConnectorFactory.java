
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IAmqpQueueConsumerConnectorFactory
		extends
			IAmqpQueueConnectorFactory<IAmqpQueueConsumerConnector<?, ?>>
{
	<Context, Message> IAmqpQueueConsumerConnector<Context, Message> create (IConfiguration configuration, Class<Message> messageClass, DataEncoder<? super Message> dataEncoder, IAmqpQueueConsumerConnectorCallback<Context, Message> callback, Context callbackContext);
}
