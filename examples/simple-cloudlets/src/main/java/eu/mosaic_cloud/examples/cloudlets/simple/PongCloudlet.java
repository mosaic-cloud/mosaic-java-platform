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
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.platform.implementations.v1.serialization.JsonDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class PongCloudlet
{
	public static class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		public CloudletCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final DestroyArguments arguments) {
			context.logger.info ("destroying cloudlet...");
			context.logger.info ("destroying queue connectors...");
			return (context.destroyConnectors (context.consumer, context.publisher));
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("cloudlet destroyed successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final InitializeArguments arguments) {
			context.logger.info ("initializing cloudlet...");
			context.logger.info ("creating queue connectors...");
			context.consumer = context.createAmqpQueueConsumerConnector ("consumer", PingMessage.class, JsonDataEncoder.create (PingMessage.class), ConsumerCallback.class);
			context.publisher = context.createAmqpQueuePublisherConnector ("publisher", PongMessage.class, JsonDataEncoder.create (PongMessage.class), PublisherCallback.class);
			context.logger.info ("initializing queue connectors...");
			return (context.initializeConnectors (context.consumer, context.publisher));
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("cloudlet initialized successfully.");
			return (Callback.SUCCESS);
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		CloudletController<Context> cloudlet;
		AmqpQueueConsumerConnector<PingMessage, Void> consumer;
		AmqpQueuePublisherConnector<PongMessage, Void> publisher;
	}
	
	static class ConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<Context, PingMessage, Void>
	{
		public ConsumerCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> consume (final Context context, final ConsumeArguments<PingMessage> arguments) {
			final PingMessage ping = arguments.message;
			context.logger.info ("received ping message with token `{}`; acknowledging...", ping);
			final PongMessage pong = new PongMessage (ping.token);
			context.logger.info ("sending pong message with token `{}`...", pong);
			context.publisher.publish (pong, null);
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("queue connector connector destroyed successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("queue connector connector initialized successfully.");
			return (Callback.SUCCESS);
		}
	}
	
	static class PublisherCallback
				extends DefaultAmqpQueuePublisherConnectorCallback<Context, PongMessage, Void>
	{
		public PublisherCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("queue connector connector destroyed successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("queue connector connector initialized successfully.");
			return (Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> publishSucceeded (final Context context, final PublishSucceededArguments<PongMessage, Void> arguments) {
			context.logger.info ("publish succeeded; exiting...");
			context.cloudlet.destroy ();
			return (Callback.SUCCESS);
		}
	}
}
