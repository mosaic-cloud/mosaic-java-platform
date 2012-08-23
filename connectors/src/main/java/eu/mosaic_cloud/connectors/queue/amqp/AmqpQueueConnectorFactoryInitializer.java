
package eu.mosaic_cloud.connectors.queue.amqp;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public final class AmqpQueueConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IAmqpQueueRawConnectorFactory.class, new IAmqpQueueRawConnectorFactory () {
			@Override
			public IAmqpQueueRawConnector create (final IConfiguration configuration)
			{
				return AmqpQueueRawConnector.create (ConnectorConfiguration.create (configuration, environment));
			}
		});
		builder.register (IAmqpQueueConsumerConnectorFactory.class, new IAmqpQueueConsumerConnectorFactory () {
			@Override
			public <TMessage> IAmqpQueueConsumerConnector<TMessage> create (final IConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final IAmqpQueueConsumerCallback<TMessage> callback)
			{
				return AmqpQueueConsumerConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder, callback);
			}
		});
		builder.register (IAmqpQueuePublisherConnectorFactory.class, new IAmqpQueuePublisherConnectorFactory () {
			@Override
			public <TMessage> IAmqpQueuePublisherConnector<TMessage> create (final IConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder)
			{
				return AmqpQueuePublisherConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder);
			}
		});
	}
	
	public static final AmqpQueueConnectorFactoryInitializer defaultInstance = new AmqpQueueConnectorFactoryInitializer ();
}
