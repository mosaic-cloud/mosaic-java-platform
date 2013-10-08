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

package eu.mosaic_cloud.platform.implementation.v2.connectors.queue.amqp;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilder;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerCallback;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerConnectorFactory;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueuePublisherConnectorFactory;
import eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueueRawConnectorFactory;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;


public final class AmqpQueueConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		builder.register (AmqpQueueRawConnectorFactory.class, new AmqpQueueRawConnectorFactory () {
			@Override
			public AmqpQueueRawConnector create (final Configuration configuration) {
				return AmqpQueueRawConnector.create (ConnectorConfiguration.create (configuration, environment));
			}
		});
		builder.register (QueueConsumerConnectorFactory.class, new QueueConsumerConnectorFactory () {
			@Override
			public <TMessage> AmqpQueueConsumerConnector<TMessage> create (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerCallback<TMessage> callback) {
				return AmqpQueueConsumerConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder, callback);
			}
		});
		builder.register (QueuePublisherConnectorFactory.class, new QueuePublisherConnectorFactory () {
			@Override
			public <TMessage> AmqpQueuePublisherConnector<TMessage> create (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder) {
				return AmqpQueuePublisherConnector.create (ConnectorConfiguration.create (configuration, environment), messageClass, messageEncoder);
			}
		});
	}
	
	public static final AmqpQueueConnectorFactoryInitializer defaultInstance = new AmqpQueueConnectorFactoryInitializer ();
}
