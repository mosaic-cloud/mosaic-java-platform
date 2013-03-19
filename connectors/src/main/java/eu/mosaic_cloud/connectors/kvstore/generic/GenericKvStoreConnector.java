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

package eu.mosaic_cloud.connectors.kvstore.generic;


import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;


/**
 * Connector for key-value distributed storage systems.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 */
public class GenericKvStoreConnector<TValue extends Object>
		extends BaseKvStoreConnector<TValue, GenericKvStoreConnectorProxy<TValue>>
{
	protected GenericKvStoreConnector (final GenericKvStoreConnectorProxy<TValue> proxy)
	{
		super (proxy);
	}
	
	/**
	 * Creates the connector.
	 * 
	 * @param configuration
	 *            the execution environment of a connector
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @param threading
	 * @return the connector
	 */
	public static <T, TExtra extends MessageEnvelope> GenericKvStoreConnector<T> create (final ConnectorConfiguration configuration, final DataEncoder<T> encoder)
	{
		final GenericKvStoreConnectorProxy<T> proxy = GenericKvStoreConnectorProxy.create (configuration, encoder);
		return new GenericKvStoreConnector<T> (proxy);
	}
}
