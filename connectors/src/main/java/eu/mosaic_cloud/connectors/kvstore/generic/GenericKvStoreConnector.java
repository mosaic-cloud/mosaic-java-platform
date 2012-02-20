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

package eu.mosaic_cloud.connectors.kvstore.generic;


import eu.mosaic_cloud.connectors.core.BaseConnectorProxy;
import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConfigProperties;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * Connector for key-value distributed storage systems .
 * 
 * @author Georgiana Macariu
 * @param <_Value_>
 *            type of stored data
 */
public class GenericKvStoreConnector<_Value_ extends Object>
		extends BaseKvStoreConnector<_Value_, GenericKvStoreConnectorProxy<_Value_>>
{
	protected GenericKvStoreConnector (final GenericKvStoreConnectorProxy<_Value_> proxy)
	{
		super (proxy);
	}
	
	/**
	 * Creates the connector.
	 * 
	 * @param configuration
	 *            the configuration parameters required by the connector. This
	 *            should also include configuration settings for the
	 *            corresponding driver.
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @return the connector
	 * @throws Throwable
	 */
	public static <T extends Object> GenericKvStoreConnector<T> create (final IConfiguration configuration, final DataEncoder<T> encoder, final ThreadingContext threading)
	{
		final String bucket = ConfigUtils.resolveParameter (configuration, ConfigProperties.getString ("KeyValueStoreConnector.1"), String.class, "");
		final String driverIdentity = ConfigUtils.resolveParameter (configuration, ConfigProperties.getString ("AllConnector.1"), String.class, "");
		final String driverEndpoint = ConfigUtils.resolveParameter (configuration, ConfigProperties.getString ("AllConnector.0"), String.class, "");
		final Channel channel = BaseConnectorProxy.createChannel (driverEndpoint, threading);
		channel.register (KeyValueSession.CONNECTOR);
		final GenericKvStoreConnectorProxy<T> proxy = GenericKvStoreConnectorProxy.create (bucket, configuration, driverIdentity, channel, encoder);
		return new GenericKvStoreConnector<T> (proxy);
	}
}
