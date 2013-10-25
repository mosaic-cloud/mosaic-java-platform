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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.queue.generic;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueuePublisherConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueuePublisherConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorVariant;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.base.Preconditions;


public final class GenericQueueConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		Preconditions.checkNotNull (delegate);
		this.register (builder, QueueConsumerConnectorFactory.class, GenericQueueConnectorFactoryInitializer.variant, true, true, new QueueConsumerConnectorFactory () {
			@Override
			public <TContext, TMessage, TExtra> GenericQueueConsumerConnector<TContext, TMessage, TExtra> create (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
				// TODO: Resolve the connector variant from the configuration!
				final ConnectorVariant variant = ConnectorVariant.fallback;
				final GenericQueueConsumerConnector.Callback<TMessage> backingCallback = new GenericQueueConsumerConnector.Callback<TMessage> ();
				final eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerConnector<TMessage> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerConnectorFactory.class, variant).create (configuration, messageClass, messageEncoder, backingCallback);
				return new GenericQueueConsumerConnector<TContext, TMessage, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext, backingCallback);
			}
		});
		this.register (builder, QueuePublisherConnectorFactory.class, GenericQueueConnectorFactoryInitializer.variant, true, true, new QueuePublisherConnectorFactory () {
			@Override
			public <TContext, TMessage, TExtra> GenericQueuePublisherConnector<TContext, TMessage, TExtra> create (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
				// TODO: Resolve the connector variant from the configuration!
				final ConnectorVariant variant = ConnectorVariant.fallback;
				final eu.mosaic_cloud.platform.v2.connectors.queue.QueuePublisherConnector<TMessage> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.platform.v2.connectors.queue.QueuePublisherConnectorFactory.class, variant).create (configuration, messageClass, messageEncoder);
				return new GenericQueuePublisherConnector<TContext, TMessage, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final GenericQueueConnectorFactoryInitializer defaultInstance = new GenericQueueConnectorFactoryInitializer ();
	public static final ConnectorVariant variant = ConnectorVariant.resolve ("eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors");
}
