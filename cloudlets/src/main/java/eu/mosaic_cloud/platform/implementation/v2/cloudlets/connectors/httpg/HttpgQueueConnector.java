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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.httpg;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorCallback.RequestedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorCallback.RespondFailedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorCallback.RespondSucceededArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgRequestMessage;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public class HttpgQueueConnector<TContext, TRequestBody, TResponseBody, TExtra>
			extends BaseConnector<eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueConnector<TRequestBody, TResponseBody>, HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>, TContext>
			implements
				eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnector<TRequestBody, TResponseBody, TExtra>
{
	@SuppressWarnings ("synthetic-access")
	public HttpgQueueConnector (final CloudletController<?> cloudlet, final eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueConnector<TRequestBody, TResponseBody> connector, final Configuration configuration, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback, final TContext context, final Callback<TRequestBody, TResponseBody> backingCallback) {
		super (cloudlet, connector, configuration, callback, context);
		backingCallback.connector = this;
	}
	
	@Override
	public CallbackCompletion<Void> respond (final HttpgResponseMessage<TResponseBody> response) {
		return this.respond (response, null);
	}
	
	@Override
	public CallbackCompletion<Void> respond (final HttpgResponseMessage<TResponseBody> response, final TExtra extra) {
		final CallbackCompletion<Void> completion = this.connector.respond (response);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
					assert (completion_ == completion);
					CallbackCompletion<Void> result;
					if (completion.getException () == null) {
						result = HttpgQueueConnector.this.callback.respondSucceeded (HttpgQueueConnector.this.context, new RespondSucceededArguments<TResponseBody, TExtra> (HttpgQueueConnector.this.cloudlet, HttpgQueueConnector.this, response, extra));
					} else {
						result = HttpgQueueConnector.this.callback.respondFailed (HttpgQueueConnector.this.context, new RespondFailedArguments<TResponseBody, TExtra> (HttpgQueueConnector.this.cloudlet, HttpgQueueConnector.this, response, completion.getException (), extra));
					}
					return result;
				}
			});
		}
		return completion;
	}
	
	protected CallbackCompletion<Void> requested (final HttpgRequestMessage<TRequestBody> request) {
		CallbackCompletion<Void> result;
		if (this.callback == null) {
			result = CallbackCompletion.createFailure (new IllegalStateException ());
		} else {
			result = this.callback.requested (this.context, new RequestedArguments<TRequestBody> (this.cloudlet, this, request));
		}
		return result;
	}
	
	public static final class Callback<TRequestBody, TResponseBody>
				implements
					eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueCallback<TRequestBody, TResponseBody>
	{
		@Override
		public CallbackCompletion<Void> requested (final HttpgRequestMessage<TRequestBody> request) {
			return this.connector.requested (request);
		}
		
		private HttpgQueueConnector<?, TRequestBody, TResponseBody, ?> connector = null;
	}
}
