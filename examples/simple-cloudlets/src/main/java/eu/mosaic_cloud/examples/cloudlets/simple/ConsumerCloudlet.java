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
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class ConsumerCloudlet
{
	public static final class AmqpConsumerCallback
			extends DefaultAmqpQueueConsumerConnectorCallback<ConsumerCloudletContext, String, Void>
	{
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final ConsumerCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments)
		{
			context.cloudlet.destroy ();
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> consume (final ConsumerCloudletContext context, final AmqpQueueConsumeCallbackArguments<String> arguments)
		{
			final String data = arguments.getMessage ();
			this.logger.info ("ConsumerCloudlet received message `{}`.", data);
			context.consumer.acknowledge (arguments.getToken ());
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final ConsumerCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("ConsumerCloudlet consumer destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final ConsumerCloudletContext context, final CallbackArguments arguments)
		{
			this.logger.info ("ConsumerCloudlet consumer initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
	
	public static final class ConsumerCloudletContext
	{
		ICloudletController<ConsumerCloudletContext> cloudlet;
		IAmqpQueueConsumerConnector<String, Void> consumer;
	}
	
	public static final class LifeCycleHandler
			extends DefaultCloudletCallback<ConsumerCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final ConsumerCloudletContext context, final CloudletCallbackArguments<ConsumerCloudletContext> arguments)
		{
			this.logger.info ("ConsumerCloudlet destroying...");
			return context.consumer.destroy ();
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final ConsumerCloudletContext context, final CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments)
		{
			this.logger.info ("ConsumerCloudlet destroyed successfully.");
			return ICallback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final ConsumerCloudletContext context, final CloudletCallbackArguments<ConsumerCloudletContext> arguments)
		{
			this.logger.info ("ConsumerCloudlet initializing...");
			context.cloudlet = arguments.getCloudlet ();
			final IConfiguration configuration = context.cloudlet.getConfiguration ();
			final IConfiguration queueConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("queue"));
			final DataEncoder<String> encoder = PojoDataEncoder.create (String.class);
			context.consumer = context.cloudlet.getConnectorFactory (IAmqpQueueConsumerConnectorFactory.class).create (queueConfiguration, String.class, encoder, new AmqpConsumerCallback (), context);
			return context.consumer.initialize ();
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final ConsumerCloudletContext context, final CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments)
		{
			this.logger.info ("ConsumerCloudlet initialized successfully.");
			return ICallback.SUCCESS;
		}
	}
}
