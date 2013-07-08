/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.connectors.implementations.v1.queue.amqp;


import eu.mosaic_cloud.connectors.implementations.v1.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueRawConnector;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueRawConnectorFactory;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;


public final class AmqpQueueConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate) {
		builder.register (IAmqpQueueRawConnectorFactory.class, new IAmqpQueueRawConnectorFactory () {
			@Override
			public IAmqpQueueRawConnector create (final Configuration configuration) {
				return AmqpQueueRawConnector.create (ConnectorConfiguration.create (configuration, environment));
			}
		});
		builder.register (IAmqpQueueConsumerConnectorFactory.class, new IAmqpQueueConsumerConnectorFactory () {
			@Override
			public <TMessage> IAmqpQueueConsumerConnector<TMessage> create (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final IAmqpQueueConsumerCallback<TMessage> callback) {
				return AmqpQueueConsumerConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder, callback);
			}
		});
		builder.register (IAmqpQueuePublisherConnectorFactory.class, new IAmqpQueuePublisherConnectorFactory () {
			@Override
			public <TMessage> IAmqpQueuePublisherConnector<TMessage> create (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder) {
				return AmqpQueuePublisherConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder);
			}
		});
	}
	
	public static final AmqpQueueConnectorFactoryInitializer defaultInstance = new AmqpQueueConnectorFactoryInitializer ();
}
