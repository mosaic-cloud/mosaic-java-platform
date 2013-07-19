/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple;


import java.util.UUID;

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultHttpgQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgQueueConnector;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgRequestMessage;
import eu.mosaic_cloud.connectors.v1.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class HttpgCloudlet
			extends DefaultCloudlet
{
	public static class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		@Override
		protected CallbackCompletion<Void> destroy (final Context context) {
			context.logger.info ("destroying cloudlet...");
			return (context.gateway.destroy ());
		}
		
		@Override
		protected CallbackCompletion<Void> initialize (final Context context) {
			context.logger.info ("initializing cloudlet...");
			context.gateway = context.createHttpgQueueConnector ("gateway", String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, GatewayCallback.class);
			return (context.gateway.initialize ());
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		HttpgQueueConnector<String, String> gateway;
		final String identity = UUID.randomUUID ().toString ();
	}
	
	public static class GatewayCallback
				extends DefaultHttpgQueueConnectorCallback<Context, String, String, Void>
	{
		@Override
		protected CallbackCompletion<Void> requested (final Context context, final HttpgRequestMessage<String> request) {
			final StringBuilder responseBody = new StringBuilder ();
			responseBody.append (String.format ("Cloudlet: %s\n", context.identity));
			responseBody.append (String.format ("HTTP version: %s\n", request.version));
			responseBody.append (String.format ("HTTP method: %s\n", request.method));
			responseBody.append (String.format ("HTTP path: %s\n", request.path));
			if (request.body != null) {
				responseBody.append ("HTTP body:\n");
				responseBody.append (request.body);
			} else
				responseBody.append ("HTTP body: empty\n");
			final HttpgResponseMessage<String> response = HttpgResponseMessage.create200 (request, responseBody.toString ());
			context.gateway.respond (response);
			return (DefaultCallback.Succeeded);
		}
	}
}
