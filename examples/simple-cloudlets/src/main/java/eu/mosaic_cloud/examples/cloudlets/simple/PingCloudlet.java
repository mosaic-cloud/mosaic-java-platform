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

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.ICloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.core.ICallback;
import eu.mosaic_cloud.platform.implementations.v1.serialization.JsonDataEncoder;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class PingCloudlet
{
	public static final class AmqpConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<PingCloudletContext, PongMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final PingCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments) {
			this.logger.info ("ackowledge succeeded; exiting...");
			context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> consume (final PingCloudletContext context, final AmqpQueueConsumeCallbackArguments<PongMessage> arguments) {
			final PongMessage pong = arguments.getMessage ();
			this.logger.info ("received pong message with key `{}`; acknowledging...", pong.getKey ());
			context.consumer.acknowledge (arguments.getToken ());
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("queue consumer connector destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("queue consumer connector initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class AmqpPublisherCallback
				extends DefaultAmqpPublisherConnectorCallback<PingCloudletContext, PingMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("queue publisher connector destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("queue publisher connector initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
				extends DefaultCloudletCallback<PingCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final PingCloudletContext context, final CloudletCallbackArguments<PingCloudletContext> arguments) {
			this.logger.info ("destroying cloudlet...");
			this.logger.info ("destroying queue connectors...");
			return CallbackCompletion.createAndChained (context.consumer.destroy (), context.publisher.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CloudletCallbackCompletionArguments<PingCloudletContext> arguments) {
			this.logger.info ("cloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final PingCloudletContext context, final CloudletCallbackArguments<PingCloudletContext> arguments) {
			this.logger.info ("initializing cloudlet...");
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration configuration = context.cloudlet.getConfiguration ();
			final IConfiguration consumerConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("consumer"));
			final IConfiguration publisherConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("publisher"));
			this.logger.info ("creating queue connectors...");
			context.consumer = context.cloudlet.getConnectorFactory (IAmqpQueueConsumerConnectorFactory.class).create (consumerConfiguration, PongMessage.class, JsonDataEncoder.create (PongMessage.class), new AmqpConsumerCallback (), context);
			context.publisher = context.cloudlet.getConnectorFactory (IAmqpQueuePublisherConnectorFactory.class).create (publisherConfiguration, PingMessage.class, JsonDataEncoder.create (PingMessage.class), new AmqpPublisherCallback (), context);
			this.logger.info ("initializing queue connectors...");
			return CallbackCompletion.createAndChained (context.consumer.initialize (), context.publisher.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CloudletCallbackCompletionArguments<PingCloudletContext> arguments) {
			this.logger.info ("cloudlet initialized successfully.");
			final PingMessage ping = new PingMessage (context.pingPongKey);
			this.logger.info ("sending ping message with key `{}`...", ping.getKey ());
			context.publisher.publish (ping, null);
			return ICallback.SUCCESS;
		}
	}
	
	public static final class PingCloudletContext
	{
		ICloudletController<PingCloudletContext> cloudlet;
		IAmqpQueueConsumerConnector<PongMessage, Void> consumer;
		final String pingPongKey = UUID.randomUUID ().toString ();
		IAmqpQueuePublisherConnector<PingMessage, Void> publisher;
	}
}
