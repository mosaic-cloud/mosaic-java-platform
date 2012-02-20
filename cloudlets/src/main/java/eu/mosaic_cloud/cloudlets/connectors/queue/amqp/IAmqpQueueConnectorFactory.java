
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.connectors.queue.IQueueConnectorFactory;


public interface IAmqpQueueConnectorFactory<C extends IAmqpQueueConnector<?>>
		extends
			IQueueConnectorFactory<C>
{}
