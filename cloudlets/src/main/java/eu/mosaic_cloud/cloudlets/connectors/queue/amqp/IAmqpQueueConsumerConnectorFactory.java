
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IAmqpQueueConsumerConnectorFactory
		extends
			IAmqpQueueConnectorFactory<IAmqpQueueConsumerConnector<?, ?>>
{
	<Context, Data> IAmqpQueueConsumerConnector<Context, Data> create (IConfiguration configuration, Class<Data> dataClass, DataEncoder<? super Data> dataEncoder);
}
