/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.kvstore.memcache;


import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.connectors.BaseConnectorProxy;
import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConfigProperties;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.interop.kvstore.MemcachedSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * Connector for key-value distributed storage systems implementing the
 * memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcacheKvStoreConnector<T extends Object>
		extends BaseKvStoreConnector<T, MemcacheKvStoreConnectorProxy<T>>
		implements
			IMemcacheKvStore<T>
{
	protected MemcacheKvStoreConnector (final MemcacheKvStoreConnectorProxy<T> proxy)
	{
		super (proxy);
	}
	
	@Override
	public CallbackCompletion<Boolean> add (final String key, final int exp, final T data)
	{
		return this.proxy.add (key, exp, data);
	}
	
	@Override
	public CallbackCompletion<Boolean> append (final String key, final T data)
	{
		return this.proxy.append (key, data);
	}
	
	@Override
	public CallbackCompletion<Boolean> cas (final String key, final T data)
	{
		return this.proxy.cas (key, data);
	}
	
	@Override
	public CallbackCompletion<Map<String, T>> getBulk (final List<String> keys)
	{
		return this.proxy.getBulk (keys);
	}
	
	@Override
	public CallbackCompletion<List<String>> list ()
	{
		return this.proxy.list ();
	}
	
	@Override
	public CallbackCompletion<Boolean> prepend (final String key, final T data)
	{
		return this.proxy.prepend (key, data);
	}
	
	@Override
	public CallbackCompletion<Boolean> replace (final String key, final int exp, final T data)
	{
		return this.proxy.replace (key, exp, data);
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final int exp, final T data)
	{
		return this.proxy.set (key, exp, data);
	}
	
	/**
	 * Creates the connector.
	 * 
	 * @param config
	 *            the configuration parameters required by the connector. This
	 *            should also include configuration settings for the
	 *            corresponding driver.
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @return the connector
	 * @throws Throwable
	 */
	public static <T extends Object> MemcacheKvStoreConnector<T> create (final IConfiguration config, final DataEncoder<T> encoder, final ThreadingContext threading)
	{
		final String bucket = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KeyValueStoreConnector.1"), String.class, "");
		final String driverIdentity = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AllConnector.1"), String.class, "");
		final String driverEndpoint = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AllConnector.0"), String.class, "");
		final Channel channel = BaseConnectorProxy.createChannel (driverEndpoint, threading);
		channel.register (KeyValueSession.CONNECTOR);
		channel.register (MemcachedSession.CONNECTOR);
		final MemcacheKvStoreConnectorProxy<T> proxy = MemcacheKvStoreConnectorProxy.create (bucket, config, driverIdentity, channel, encoder);
		return new MemcacheKvStoreConnector<T> (proxy);
	}
}
