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

import eu.mosaic_cloud.cloudlets.connectors.httpg.HttpgQueueRequestedCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.httpg.IHttpgQueueConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultHttpgQueueConnectorCallback;
import eu.mosaic_cloud.connectors.httpg.HttpgRequestMessage;
import eu.mosaic_cloud.connectors.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.connectors.httpg.IHttpgQueueConnector;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class HttpgCloudlet
{
	public static final class Callbacks
			extends DefaultCloudletCallback<Context>
	{
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final CloudletCallbackArguments<Context> arguments)
		{
			this.logger.info ("destroying cloudlet...");
			return (context.gateway.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final CloudletCallbackArguments<Context> arguments)
		{
			this.logger.info ("initializing cloudlet...");
			context.identity = UUID.randomUUID ().toString ();
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration cloudletConfiguration = context.cloudlet.getConfiguration ();
			final IConfiguration gatewayConfiguration = cloudletConfiguration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("gateway"));
			context.gateway = context.cloudlet.getConnectorFactory (IHttpgQueueConnectorFactory.class).create (gatewayConfiguration, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, new GatewayCallbacks (), context);
			return (context.gateway.initialize ());
		}
	}
	
	public static final class Context
	{
		ICloudletController<Context> cloudlet;
		IHttpgQueueConnector<String, String> gateway;
		String identity;
	}
	
	public static final class GatewayCallbacks
			extends DefaultHttpgQueueConnectorCallback<Context, String, String, Void>
	{
		@Override
		public CallbackCompletion<Void> requested (final Context context, final HttpgQueueRequestedCallbackArguments<String> arguments)
		{
			final HttpgRequestMessage<String> request = arguments.getRequest ();
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
			return (ICallback.SUCCESS);
		}
	}
}
