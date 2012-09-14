/*
 * #%L
 * mosaic-examples-simple-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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


import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.JsonDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class PongCloudlet
{
	public static final class AmqpConsumerCallback
			extends DefaultAmqpQueueConsumerConnectorCallback<PongCloudletContext, PingMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> consume (final PongCloudletContext context, final AmqpQueueConsumeCallbackArguments<PingMessage> arguments)
		{
			// NOTE: retrieve message data
			final PingMessage ping = arguments.getMessage ();
			this.logger.info ("received ping message with key `{}`; acknowledging...", ping.getKey ());
			final PongMessage pong = new PongMessage (ping.getKey (), null);
			this.logger.info ("sending pong message with key `{}`...", pong.getKey ());
			context.publisher.publish (pong, null);
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PongCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("queue consumer connector destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PongCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("queue consumer connector initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class AmqpPublisherCallback
			extends DefaultAmqpPublisherConnectorCallback<PongCloudletContext, PongMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PongCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("queue publisher connector destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PongCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("queue publisher connector initialized successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> publishSucceeded (final PongCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments)
		{
			this.logger.info ("publish succeeded; exiting...");
			context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
			extends DefaultCloudletCallback<PongCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final PongCloudletContext context, final CloudletCallbackArguments<PongCloudletContext> arguments)
		{
			this.logger.info ("destroying cloudlet...");
			this.logger.info ("destroying queue connectors...");
			return CallbackCompletion.createAndChained (context.consumer.destroy (), context.publisher.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PongCloudletContext context, final CloudletCallbackCompletionArguments<PongCloudletContext> arguments)
		{
			this.logger.info ("cloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final PongCloudletContext context, final CloudletCallbackArguments<PongCloudletContext> arguments)
		{
			this.logger.info ("initializing cloudlet...");
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration configuration = context.cloudlet.getConfiguration ();
			final IConfiguration consumerConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("consumer"));
			final IConfiguration publisherConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("publisher"));
			this.logger.info ("creating queue connectors...");
			context.consumer = context.cloudlet.getConnectorFactory (IAmqpQueueConsumerConnectorFactory.class).create (consumerConfiguration, PingMessage.class, JsonDataEncoder.create (PingMessage.class), new AmqpConsumerCallback (), context);
			context.publisher = context.cloudlet.getConnectorFactory (IAmqpQueuePublisherConnectorFactory.class).create (publisherConfiguration, PongMessage.class, JsonDataEncoder.create (PongMessage.class), new AmqpPublisherCallback (), context);
			this.logger.info ("initializing queue connectors...");
			return CallbackCompletion.createAndChained (context.consumer.initialize (), context.publisher.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PongCloudletContext context, final CloudletCallbackCompletionArguments<PongCloudletContext> arguments)
		{
			this.logger.info ("cloudlet initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class PongCloudletContext
	{
		ICloudletController<PongCloudletContext> cloudlet;
		IAmqpQueueConsumerConnector<PingMessage, Void> consumer;
		IAmqpQueuePublisherConnector<PongMessage, Void> publisher;
	}
}
