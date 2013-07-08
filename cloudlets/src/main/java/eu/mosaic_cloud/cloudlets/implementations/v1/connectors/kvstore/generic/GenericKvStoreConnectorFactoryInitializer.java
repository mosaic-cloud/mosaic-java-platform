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

package eu.mosaic_cloud.cloudlets.implementations.v1.connectors.kvstore.generic;


import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.v1.core.ConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.ConnectorsFactoryBuilder;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;

import com.google.common.base.Preconditions;


public final class GenericKvStoreConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilder builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		Preconditions.checkNotNull (delegate);
		builder.register (KvStoreConnectorFactory.class, new KvStoreConnectorFactory () {
			@Override
			public <TContext, TValue, TExtra> GenericKvStoreConnector<TContext, TValue, TExtra> create (final Configuration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext) {
				final eu.mosaic_cloud.connectors.implementations.v1.kvstore.generic.GenericKvStoreConnector<TValue> backingConnector = (eu.mosaic_cloud.connectors.implementations.v1.kvstore.generic.GenericKvStoreConnector<TValue>) delegate.getConnectorFactory (eu.mosaic_cloud.connectors.v1.kvstore.KvStoreConnectorFactory.class).create (configuration, valueClass, valueEncoder);
				return new GenericKvStoreConnector<TContext, TValue, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final GenericKvStoreConnectorFactoryInitializer defaultInstance = new GenericKvStoreConnectorFactoryInitializer ();
}
