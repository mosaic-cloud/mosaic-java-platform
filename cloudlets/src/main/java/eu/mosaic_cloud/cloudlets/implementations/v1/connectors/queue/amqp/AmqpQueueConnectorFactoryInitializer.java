/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.implementations.v1.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.v1.core.ConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactory;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;

import com.google.common.base.Preconditions;


public final class AmqpQueueConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilder builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate) {
		Preconditions.checkNotNull (delegate);
		builder.register (IAmqpQueueConsumerConnectorFactory.class, new IAmqpQueueConsumerConnectorFactory () {
			@Override
			public <TContext, Message, TExtra> IAmqpQueueConsumerConnector<Message, TExtra> create (final Configuration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder, final AmqpQueueConsumerConnectorCallback<TContext, Message, TExtra> callback, final TContext callbackContext) {
				final AmqpQueueConsumerConnector.Callback<Message> backingCallback = new AmqpQueueConsumerConnector.Callback<Message> ();
				final eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueConsumerConnector<Message> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.connectors.v1.queue.amqp.AmqpQueueConsumerConnectorFactory.class).create (configuration, messageClass, messageEncoder, backingCallback);
				return new AmqpQueueConsumerConnector<TContext, Message, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext, backingCallback);
			}
		});
		builder.register (IAmqpQueuePublisherConnectorFactory.class, new IAmqpQueuePublisherConnectorFactory () {
			@Override
			public <TContext, Message, TExtra> IAmqpQueuePublisherConnector<Message, TExtra> create (final Configuration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder, final AmqpQueuePublisherConnectorCallback<TContext, Message, TExtra> callback, final TContext callbackContext) {
				final eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueuePublisherConnector<Message> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.connectors.v1.queue.amqp.AmqpQueuePublisherConnectorFactory.class).create (configuration, messageClass, messageEncoder);
				return new AmqpQueuePublisherConnector<TContext, Message, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final AmqpQueueConnectorFactoryInitializer defaultInstance = new AmqpQueueConnectorFactoryInitializer ();
}
