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

package eu.mosaic_cloud.platform.implementation.v2.connectors.httpg;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilder;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueCallback;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueConnectorFactory;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;


public final class HttpgQueueConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		builder.register (HttpgQueueConnectorFactory.class, new HttpgQueueConnectorFactory () {
			@Override
			public <TRequestBody, TResponseBody> eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueConnector<TRequestBody, TResponseBody> create (final Configuration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueCallback<TRequestBody, TResponseBody> callback) {
				return HttpgQueueConnector.create (ConnectorConfiguration.create (configuration, environment), requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback);
			}
		});
	}
	
	public static final HttpgQueueConnectorFactoryInitializer defaultInstance = new HttpgQueueConnectorFactoryInitializer ();
}
