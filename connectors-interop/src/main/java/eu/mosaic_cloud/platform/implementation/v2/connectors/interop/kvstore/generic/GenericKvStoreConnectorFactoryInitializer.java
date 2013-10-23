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

package eu.mosaic_cloud.platform.implementation.v2.connectors.interop.kvstore.generic;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;
import eu.mosaic_cloud.platform.v2.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;


public final class GenericKvStoreConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		builder.register (KvStoreConnectorFactory.class, new KvStoreConnectorFactory () {
			@Override
			public <TValue> GenericKvStoreConnector<TValue> create (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder) {
				return GenericKvStoreConnector.create (ConnectorConfiguration.create (configuration, environment), valueEncoder);
			}
		});
	}
	
	public static final GenericKvStoreConnectorFactoryInitializer defaultInstance = new GenericKvStoreConnectorFactoryInitializer ();
}
