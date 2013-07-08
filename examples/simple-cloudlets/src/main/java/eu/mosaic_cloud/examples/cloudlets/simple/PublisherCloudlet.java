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
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.core.ICallback;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.Logger;


public class PublisherCloudlet
{
	private static CallbackCompletion<Void> maybePushMessage (final PublisherCloudletContext context) {
		{
			// FIXME: DON'T DO THIS IN YOUR CODE... This is for throttling...
			Threading.sleep (context.delay);
		}
		if (context.count < context.limit) {
			final String data = String.format ("Test message %d! (%s)", Integer.valueOf (context.count), UUID.randomUUID ().toString ());
			context.logger.info ("PublisherCloudlet sending message `{}`.", data);
			context.publisher.publish (data, null);
			context.count += 1;
		} else {
			context.cloudlet.destroy ();
		}
		return ICallback.SUCCESS;
	}
	
	public static final class AmqpPublisherCallback
				extends DefaultAmqpPublisherConnectorCallback<PublisherCloudletContext, String, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PublisherCloudletContext context, final CallbackArguments arguments) {
			context.logger.info ("PublisherCloudlet publisher destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PublisherCloudletContext context, final CallbackArguments arguments) {
			context.logger.info ("PublisherCloudlet publisher initialized successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> publishSucceeded (final PublisherCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments) {
			return PublisherCloudlet.maybePushMessage (context);
		}
	}
	
	public static final class LifeCycleHandler
				extends DefaultCloudletCallback<PublisherCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final PublisherCloudletContext context, final CloudletCallbackArguments<PublisherCloudletContext> arguments) {
			context.logger.info ("PublisherCloudlet destroying...");
			return context.publisher.destroy ();
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final PublisherCloudletContext context, final CloudletCallbackCompletionArguments<PublisherCloudletContext> arguments) {
			context.logger.info ("PublisherCloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final PublisherCloudletContext context, final CloudletCallbackArguments<PublisherCloudletContext> arguments) {
			context.cloudlet = arguments.getCloudlet ();
			context.logger = this.logger;
			context.logger.info ("PublisherCloudlet initializing...");
			final Configuration configuration = context.cloudlet.getConfiguration ();
			final Configuration queueConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("publisher"));
			context.publisher = context.cloudlet.getConnectorFactory (IAmqpQueuePublisherConnectorFactory.class).create (queueConfiguration, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, new AmqpPublisherCallback (), context);
			return context.publisher.initialize ();
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final PublisherCloudletContext context, final CloudletCallbackCompletionArguments<PublisherCloudletContext> arguments) {
			context.logger.info ("PublisherCloudlet initialized successfully.");
			return (PublisherCloudlet.maybePushMessage (context));
		}
	}
	
	public static final class PublisherCloudletContext
	{
		CloudletController<PublisherCloudletContext> cloudlet;
		int count = 0;
		int delay = 100;
		int limit = 1000;
		Logger logger;
		IAmqpQueuePublisherConnector<String, Void> publisher;
	}
}
