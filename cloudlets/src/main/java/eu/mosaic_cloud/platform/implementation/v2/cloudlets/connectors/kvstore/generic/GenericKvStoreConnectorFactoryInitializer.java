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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.kvstore.generic;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorVariant;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.base.Preconditions;


public final class GenericKvStoreConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		Preconditions.checkNotNull (delegate);
		this.register (builder, KvStoreConnectorFactory.class, GenericKvStoreConnectorFactoryInitializer.variant, true, true, new KvStoreConnectorFactory () {
			@Override
			public <TContext, TValue, TExtra> GenericKvStoreConnector<TContext, TValue, TExtra> create (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext) {
				// TODO: Resolve the connector variant from the configuration!
				final ConnectorVariant variant = ConnectorVariant.fallback;
				final eu.mosaic_cloud.platform.implementation.v2.connectors.interop.kvstore.generic.GenericKvStoreConnector<TValue> backingConnector = (eu.mosaic_cloud.platform.implementation.v2.connectors.interop.kvstore.generic.GenericKvStoreConnector<TValue>) delegate.getConnectorFactory (eu.mosaic_cloud.platform.v2.connectors.kvstore.KvStoreConnectorFactory.class, variant).create (configuration, valueClass, valueEncoder);
				return new GenericKvStoreConnector<TContext, TValue, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final GenericKvStoreConnectorFactoryInitializer defaultInstance = new GenericKvStoreConnectorFactoryInitializer ();
	public static final ConnectorVariant variant = ConnectorVariant.resolve ("eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors");
}
