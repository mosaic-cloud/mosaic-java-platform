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


import java.util.UUID;

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;
import eu.mosaic_cloud.platform.core.utils.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class PublisherCloudlet
{
	public static final class AmqpPublisherCallback
			extends DefaultAmqpPublisherConnectorCallback<PublisherCloudletContext, String, MessageEnvelope>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PublisherCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("PublisherCloudlet publisher destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PublisherCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("PublisherCloudlet publisher initialized successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> publishSucceeded (final PublisherCloudletContext context, final GenericCallbackCompletionArguments<MessageEnvelope> arguments)
		{
			context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
			extends DefaultCloudletCallback<PublisherCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final PublisherCloudletContext context, final CloudletCallbackArguments<PublisherCloudletContext> arguments)
		{
			this.logger.info ("PublisherCloudlet destroying...");
			return context.publisher.destroy ();
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PublisherCloudletContext context, final CloudletCallbackCompletionArguments<PublisherCloudletContext> arguments)
		{
			this.logger.info ("PublisherCloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final PublisherCloudletContext context, final CloudletCallbackArguments<PublisherCloudletContext> arguments)
		{
			this.logger.info ("PublisherCloudlet initializing...");
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration configuration = context.cloudlet.getConfiguration ();
			final IConfiguration queueConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("queue"));
			context.publisher = context.cloudlet.getConnectorFactory (IAmqpQueuePublisherConnectorFactory.class).create (queueConfiguration, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, new AmqpPublisherCallback (), context);
			return context.publisher.initialize ();
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PublisherCloudletContext context, final CloudletCallbackCompletionArguments<PublisherCloudletContext> arguments)
		{
			this.logger.info ("PublisherCloudlet initialized successfully.");
			context.publisher.publish (String.format ("Test message! (%s)", UUID.randomUUID ().toString ()), null);
			return ICallback.SUCCESS;
		}
	}
	
	public static final class PublisherCloudletContext
	{
		ICloudletController<PublisherCloudletContext> cloudlet;
		IAmqpQueuePublisherConnector<String, MessageEnvelope> publisher;
	}
}
