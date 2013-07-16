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


import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public class ConsumerCloudlet
			extends DefaultCloudlet
{
	static CallbackCompletion<Void> maybeContinue (final Context context) {
		// FIXME: DON'T DO THIS IN YOUR CODE... This is for throttling...
		Threading.sleep (context.delay);
		//----		
		context.count += 1;
		if (context.count >= context.limit)
			context.cloudlet.destroy ();
		return (DefaultCallback.Succeeded);
	}
	
	public static class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		public CloudletCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final DestroyArguments arguments) {
			context.logger.info ("ConsumerCloudlet destroying...");
			return (context.connector.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("ConsumerCloudlet destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final InitializeArguments arguments) {
			context.logger.info ("ConsumerCloudlet initializing...");
			context.connector = context.createAmqpQueueConsumerConnector ("connector", String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, ConnectorCallback.class);
			return (context.connector.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("ConsumerCloudlet initialized successfully.");
			return (DefaultCallback.Succeeded);
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		AmqpQueueConsumerConnector<String, Void> connector;
		int count = 0;
		final int delay = 50;
		final int limit = 10000;
	}
	
	static class ConnectorCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<Context, String, Void>
	{
		public ConnectorCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final Context context, final AcknowledgeSucceededArguments<Void> arguments) {
			return (ConsumerCloudlet.maybeContinue (context));
		}
		
		@Override
		public CallbackCompletion<Void> consume (final Context context, final ConsumeArguments<String> arguments) {
			final String data = arguments.message;
			context.logger.info ("ConsumerCloudlet received message `{}`.", data);
			context.connector.acknowledge (arguments.token);
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("ConsumerCloudlet connector destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("ConsumerCloudlet connector initialized successfully.");
			return (DefaultCallback.Succeeded);
		}
	}
}
