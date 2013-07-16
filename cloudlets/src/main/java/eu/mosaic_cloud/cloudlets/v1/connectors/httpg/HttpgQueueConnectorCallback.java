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

package eu.mosaic_cloud.cloudlets.v1.connectors.httpg;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.QueueConnectorCallback;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgRequestMessage;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface HttpgQueueConnectorCallback<TContext extends Object, TRequestBody extends Object, TResponseBody extends Object, TExtra extends Object>
			extends
				QueueConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> requested (TContext context, RequestedArguments<TRequestBody> arguments);
	
	public abstract CallbackCompletion<Void> respondFailed (TContext context, RespondFailedArguments<TResponseBody, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> respondSucceeded (TContext context, RespondSucceededArguments<TResponseBody, TExtra> arguments);
	
	public static final class RequestedArguments<TRequestBody extends Object>
				extends ConnectorCallbackArguments
	{
		public RequestedArguments (final CloudletController<?> cloudlet, final Connector connector, final HttpgRequestMessage<TRequestBody> request) {
			super (cloudlet, connector);
			this.request = request;
		}
		
		public final HttpgRequestMessage<TRequestBody> request;
	}
	
	public static final class RespondFailedArguments<TResponseBody extends Object, TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public RespondFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final HttpgResponseMessage<TResponseBody> response, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.response = response;
		}
		
		public final HttpgResponseMessage<TResponseBody> response;
	}
	
	public static final class RespondSucceededArguments<TResponseBody extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public RespondSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final HttpgResponseMessage<TResponseBody> response, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.response = response;
		}
		
		public final HttpgResponseMessage<TResponseBody> response;
	}
}
