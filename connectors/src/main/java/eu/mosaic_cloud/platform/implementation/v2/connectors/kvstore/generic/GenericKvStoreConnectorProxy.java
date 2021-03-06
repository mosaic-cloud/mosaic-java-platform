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

package eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore.generic;


import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.implementation.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore.BaseKvStoreConnectorProxy;
import eu.mosaic_cloud.platform.implementation.v2.connectors.tools.ConfigProperties;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Proxy for the driver for key-value distributed storage systems. This is used by the {@link GenericKvStoreConnector} to
 * communicate with a key-value store driver.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 */
public final class GenericKvStoreConnectorProxy<TValue extends Object>
			extends BaseKvStoreConnectorProxy<TValue>
{
	protected GenericKvStoreConnectorProxy (final ConnectorConfiguration configuration, final DataEncoder<TValue> encoder) {
		super (configuration, encoder);
		this.bucket = super.configuration.getConfigParameter (ConfigProperties.GenericKvStoreConnector_1, String.class, "");
		this.transcript.traceDebugging ("created generic kv store connector proxy for bucket `%s`.", this.bucket);
	}
	
	@Override
	public CallbackCompletion<Void> initialize () {
		this.transcript.traceDebugging ("initializing proxy...");
		final InitRequest.Builder requestBuilder = InitRequest.newBuilder ();
		requestBuilder.setToken (this.generateToken ());
		requestBuilder.setBucket (this.bucket);
		return (this.connect (KeyValueSession.CONNECTOR, new Message (KeyValueMessage.ACCESS, requestBuilder.build ())));
	}
	
	@Override
	protected String getDefaultDriverGroup () {
		return (ConfigProperties.GenericKvStoreConnector_0);
	}
	
	protected final String bucket;
	
	/**
	 * Returns a proxy for key-value distributed storage systems.
	 * 
	 * @param configuration
	 *            the execution environment of a connector
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in the key-value store
	 * @return the proxy
	 */
	public static <TValue extends Object> GenericKvStoreConnectorProxy<TValue> create (final ConnectorConfiguration configuration, final DataEncoder<TValue> encoder) {
		final GenericKvStoreConnectorProxy<TValue> proxy = new GenericKvStoreConnectorProxy<TValue> (configuration, encoder);
		return (proxy);
	}
}
