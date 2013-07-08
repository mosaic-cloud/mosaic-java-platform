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
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.core.ICallback;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.Logger;


public class ConsumerCloudlet
{
	public static final class AmqpConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<ConsumerCloudletContext, String, Void>
	{
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final ConsumerCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments) {
			{
				// FIXME: DON'T DO THIS IN YOUR CODE... This is for throttling...
				Threading.sleep (context.delay);
			}
			context.count += 1;
			if (context.count >= context.limit)
				context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> consume (final ConsumerCloudletContext context, final AmqpQueueConsumeCallbackArguments<String> arguments) {
			final String data = arguments.getMessage ();
			context.logger.info ("ConsumerCloudlet received message `{}`.", data);
			context.consumer.acknowledge (arguments.getToken ());
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final ConsumerCloudletContext context, final CallbackArguments arguments) {
			context.logger.info ("ConsumerCloudlet consumer destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final ConsumerCloudletContext context, final CallbackArguments arguments) {
			context.logger.info ("ConsumerCloudlet consumer initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class ConsumerCloudletContext
	{
		CloudletController<ConsumerCloudletContext> cloudlet;
		IAmqpQueueConsumerConnector<String, Void> consumer;
		int count = 0;
		int delay = 50;
		int limit = 10000;
		Logger logger;
	}
	
	public static final class LifeCycleHandler
				extends DefaultCloudletCallback<ConsumerCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final ConsumerCloudletContext context, final CloudletCallbackArguments<ConsumerCloudletContext> arguments) {
			context.logger.info ("ConsumerCloudlet destroying...");
			return context.consumer.destroy ();
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final ConsumerCloudletContext context, final CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments) {
			context.logger.info ("ConsumerCloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final ConsumerCloudletContext context, final CloudletCallbackArguments<ConsumerCloudletContext> arguments) {
			context.cloudlet = arguments.getCloudlet ();
			context.logger = this.logger;
			context.logger.info ("ConsumerCloudlet initializing...");
			final Configuration configuration = context.cloudlet.getConfiguration ();
			final Configuration queueConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("consumer"));
			context.consumer = context.cloudlet.getConnectorFactory (IAmqpQueueConsumerConnectorFactory.class).create (queueConfiguration, String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, new AmqpConsumerCallback (), context);
			return context.consumer.initialize ();
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final ConsumerCloudletContext context, final CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments) {
			context.logger.info ("ConsumerCloudlet initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
}
