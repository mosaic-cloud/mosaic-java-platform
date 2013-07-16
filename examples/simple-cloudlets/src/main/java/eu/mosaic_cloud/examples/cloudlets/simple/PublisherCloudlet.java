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

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public class PublisherCloudlet
			extends DefaultCloudlet
{
	static CallbackCompletion<Void> maybeContinue (final Context context) {
		// FIXME: DON'T DO THIS IN YOUR CODE... This is for throttling...
		Threading.sleep (context.delay);
		//----
		if (context.count < context.limit) {
			final String data = String.format ("Test message %d! (%s)", Integer.valueOf (context.count), UUID.randomUUID ().toString ());
			context.logger.info ("PublisherCloudlet sending message `{}`.", data);
			context.connector.publish (data, null);
			context.count += 1;
		} else
			context.cloudlet.destroy ();
		return (Callback.SUCCESS);
	}
	
	public static class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		public CloudletCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final DestroyArguments arguments) {
			context.logger.info ("PublisherCloudlet destroying...");
			return (context.connector.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("PublisherCloudlet destroyed successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final InitializeArguments arguments) {
			context.logger.info ("PublisherCloudlet initializing...");
			context.connector = context.createAmqpQueuePublisherConnector ("connector", String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, ConnectorCallback.class);
			return (context.connector.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("PublisherCloudlet initialized successfully.");
			return (PublisherCloudlet.maybeContinue (context));
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		AmqpQueuePublisherConnector<String, Void> connector;
		int count = 0;
		final int delay = 100;
		final int limit = 1000;
	}
	
	static class ConnectorCallback
				extends DefaultAmqpQueuePublisherConnectorCallback<Context, String, Void>
	{
		public ConnectorCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("PublisherCloudlet connector destroyed successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("PublisherCloudlet connector initialized successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> publishSucceeded (final Context context, final PublishSucceededArguments<String, Void> arguments) {
			return (PublisherCloudlet.maybeContinue (context));
		}
	}
}
