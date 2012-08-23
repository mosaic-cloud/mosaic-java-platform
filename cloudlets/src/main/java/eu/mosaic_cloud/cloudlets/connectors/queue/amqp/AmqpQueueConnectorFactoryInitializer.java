
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;


public final class AmqpQueueConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (delegate);
		builder.register (IAmqpQueueConsumerConnectorFactory.class, new IAmqpQueueConsumerConnectorFactory () {
			@Override
			public <TContext, Message, TExtra> IAmqpQueueConsumerConnector<Message, TExtra> create (final IConfiguration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder, final IAmqpQueueConsumerConnectorCallback<TContext, Message, TExtra> callback, final TContext callbackContext)
			{
				final AmqpQueueConsumerConnector.Callback<Message> backingCallback = new AmqpQueueConsumerConnector.Callback<Message> ();
				final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<Message> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory.class).create (configuration, messageClass, messageEncoder, backingCallback);
				return new AmqpQueueConsumerConnector<TContext, Message, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext, backingCallback);
			}
		});
		builder.register (IAmqpQueuePublisherConnectorFactory.class, new IAmqpQueuePublisherConnectorFactory () {
			@Override
			public <TContext, Message, TExtra> IAmqpQueuePublisherConnector<Message, TExtra> create (final IConfiguration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder, final IAmqpQueuePublisherConnectorCallback<TContext, Message, TExtra> callback, final TContext callbackContext)
			{
				final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnector<Message> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory.class).create (configuration, messageClass, messageEncoder);
				return new AmqpQueuePublisherConnector<TContext, Message, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final AmqpQueueConnectorFactoryInitializer defaultInstance = new AmqpQueueConnectorFactoryInitializer ();
}
