/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;


public final class MemcacheKvStoreConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (delegate);
		builder.register (IMemcacheKvStoreConnectorFactory.class, new IMemcacheKvStoreConnectorFactory () {
			@Override
			public <TContext, TValue, TExtra> IMemcacheKvStoreConnector<TValue, TExtra> create (final IConfiguration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext)
			{
				final eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue> backingConnector = (eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue>) delegate.getConnectorFactory (eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnectorFactory.class).create (configuration, valueClass, valueEncoder);
				return new MemcacheKvStoreConnector<TContext, TValue, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final MemcacheKvStoreConnectorFactoryInitializer defaultInstance = new MemcacheKvStoreConnectorFactoryInitializer ();
}