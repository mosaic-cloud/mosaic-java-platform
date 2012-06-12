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


import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;


/**
 * Factory for creating memcached key-value store connectors.
 * 
 * @author Ciprian Craciun
 * 
 */
public interface IMemcacheKvStoreConnectorFactory
		extends
			IConnectorFactory<IMemcacheKvStoreConnector<?, ?>>
{
	<TContext, TValue, TExtra extends MessageEnvelope> IMemcacheKvStoreConnector<TValue, TExtra> create (IConfiguration configuration, Class<TValue> valueClass, DataEncoder<TValue> valueEncoder, IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra> callback, TContext callbackContext);
}
