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


public class PingCloudlet
{
	public static final class AmqpConsumerCallback
			extends DefaultAmqpQueueConsumerConnectorCallback<PingCloudletContext, PongMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final PingCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments)
		{
			context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> consume (final PingCloudletContext context, final AmqpQueueConsumeCallbackArguments<PongMessage, Void> arguments)
		{
			final PongMessage data = arguments.getMessage ();
			final String key = data.getKey ();
			final PingPongData value = data.getValue ();
			this.logger.info ("Ping Cloudlet received key-value pair: (" + key + ", " + value + ")");
			context.consumer.acknowledge (arguments.getDelivery ());
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("Ping Cloudlet consumer was destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("Ping Cloudlet consumer initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class AmqpPublisherCallback
			extends DefaultAmqpPublisherConnectorCallback<PingCloudletContext, PingMessage, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("Ping Cloudlet publisher was destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("Ping Cloudlet publisher initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
			extends DefaultCloudletCallback<PingCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final PingCloudletContext context, final CloudletCallbackArguments<PingCloudletContext> arguments)
		{
			this.logger.info ("Ping Cloudlet is being destroyed.");
			return CallbackCompletion.createAndChained (context.consumer.destroy (), context.publisher.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PingCloudletContext context, final CloudletCallbackCompletionArguments<PingCloudletContext> arguments)
		{
			this.logger.info ("Ping Cloudlet was destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final PingCloudletContext context, final CloudletCallbackArguments<PingCloudletContext> arguments)
		{
			this.logger.info ("Ping Cloudlet is being initialized.");
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration configuration = context.cloudlet.getConfiguration ();
			final IConfiguration queueConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("queue"));
			context.consumer = context.cloudlet.getConnectorFactory (IAmqpQueueConsumerConnectorFactory.class).create (queueConfiguration, PongMessage.class, new JsonDataEncoder<PongMessage> (PongMessage.class), new AmqpConsumerCallback (), context);
			context.publisher = context.cloudlet.getConnectorFactory (IAmqpQueuePublisherConnectorFactory.class).create (queueConfiguration, PingMessage.class, new JsonDataEncoder<PingMessage> (PingMessage.class), new AmqpPublisherCallback (), context);
			return CallbackCompletion.createAndChained (context.consumer.initialize (), context.publisher.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PingCloudletContext context, final CloudletCallbackCompletionArguments<PingCloudletContext> arguments)
		{
			this.logger.info ("Ping Cloudlet initialized successfully.");
			final PingMessage data = new PingMessage ("pingpong");
			context.publisher.publish (data, null);
			return ICallback.SUCCESS;
		}
	}
	
	public static final class PingCloudletContext
	{
		ICloudletController<PingCloudletContext> cloudlet;
		IAmqpQueueConsumerConnector<PongMessage, Void> consumer;
		IAmqpQueuePublisherConnector<PingMessage, Void> publisher;
	}
}
