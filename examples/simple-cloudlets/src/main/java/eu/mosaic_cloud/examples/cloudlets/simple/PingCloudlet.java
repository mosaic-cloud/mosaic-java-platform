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

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.platform.implementations.v1.serialization.JsonDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class PingCloudlet
			extends DefaultCloudlet
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
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final InitializeArguments arguments) {
			context.logger.info ("initializing cloudlet...");
			context.logger.info ("creating queue connectors...");
			context.consumer = context.createAmqpQueueConsumerConnector ("consumer", PongMessage.class, JsonDataEncoder.create (PongMessage.class), ConsumerCallback.class);
			context.publisher = context.createAmqpQueuePublisherConnector ("publisher", PingMessage.class, JsonDataEncoder.create (PingMessage.class), PublisherCallback.class);
			context.logger.info ("initializing queue connectors...");
			return (context.initializeConnectors (context.consumer, context.publisher));
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("cloudlet initialized successfully.");
			final PingMessage ping = new PingMessage (context.token);
			context.logger.info ("sending ping message with token `{}`...", ping.token);
			context.publisher.publish (ping, null);
			return (DefaultCallback.Succeeded);
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		AmqpQueueConsumerConnector<PongMessage, Void> consumer;
		AmqpQueuePublisherConnector<PingMessage, Void> publisher;
		final String token = UUID.randomUUID ().toString ();
	}
	
	static class ConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<Context, PongMessage, Void>
	{
		public ConsumerCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final Context context, final AcknowledgeSucceededArguments<Void> arguments) {
			context.logger.info ("ackowledge succeeded; exiting...");
			context.cloudlet.destroy ();
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> consume (final Context context, final ConsumeArguments<PongMessage> arguments) {
			final PongMessage pong = arguments.message;
			context.logger.info ("received pong message with token `{}`; acknowledging...", pong.token);
			context.consumer.acknowledge (arguments.token);
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("queue connector connector destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("queue connector connector initialized successfully.");
			return (DefaultCallback.Succeeded);
		}
	}
	
	static class PublisherCallback
				extends DefaultAmqpQueuePublisherConnectorCallback<Context, PingMessage, Void>
	{
		public PublisherCallback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final DestroySucceededArguments arguments) {
			context.logger.info ("queue connector connector destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final InitializeSucceededArguments arguments) {
			context.logger.info ("queue connector connector initialized successfully.");
			return (DefaultCallback.Succeeded);
		}
	}
}
