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

package eu.mosaic_cloud.connectors.implementations.v1.httpg;


import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgQueueCallback;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;


public class HttpgQueueConnector<TRequestBody, TResponseBody>
			implements
				eu.mosaic_cloud.connectors.v1.httpg.HttpgQueueConnector<TRequestBody, TResponseBody>
{
	protected HttpgQueueConnector (final HttpgQueueConnectorProxy<TRequestBody, TResponseBody> proxy) {
		super ();
		Preconditions.checkNotNull (proxy);
		this.proxy = proxy;
		this.transcript = Transcript.create (this, true);
		this.transcript.traceDebugging ("creating the connector of type `%{object:class}`.", this);
		this.transcript.traceDebugging ("using the underlying connector proxy `%{object}`...", this.proxy);
	}
	
	@Override
	public CallbackCompletion<Void> destroy () {
		return this.proxy.destroy ();
	}
	
	@Override
	public CallbackCompletion<Void> initialize () {
		return this.proxy.initialize ();
	}
	
	@Override
	public CallbackCompletion<Void> respond (final HttpgResponseMessage<TResponseBody> response) {
		return this.proxy.respond (response);
	}
	
	protected final HttpgQueueConnectorProxy<TRequestBody, TResponseBody> proxy;
	protected final Transcript transcript;
	
	public static <TRequestBody, TResponseBody> HttpgQueueConnector<TRequestBody, TResponseBody> create (final ConnectorConfiguration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueCallback<TRequestBody, TResponseBody> callback) {
		final HttpgQueueConnectorProxy<TRequestBody, TResponseBody> proxy = HttpgQueueConnectorProxy.create (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback);
		return (new HttpgQueueConnector<TRequestBody, TResponseBody> (proxy));
	}
}
