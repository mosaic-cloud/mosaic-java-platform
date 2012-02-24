
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;

import eu.mosaic_cloud.cloudlets.connectors.queue.IQueueConnectorFactory;

public interface IAmqpQueueConnectorFactory<Context extends IAmqpQueueConnector<?>> extends
        IQueueConnectorFactory<Context> {
}
