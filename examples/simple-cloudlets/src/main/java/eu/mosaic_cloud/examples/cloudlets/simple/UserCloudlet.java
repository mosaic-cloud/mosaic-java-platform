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


import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.serialization.SerializedDataEncoder;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class UserCloudlet
{
	public static final class AmqpConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<UserCloudletContext, AuthenticationToken, Void>
	{
		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded (final UserCloudletContext context, final GenericCallbackCompletionArguments<Void> arguments) {
			context.cloudlet.destroy ();
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> consume (final UserCloudletContext context, final AmqpQueueConsumeCallbackArguments<AuthenticationToken> arguments) {
			final AuthenticationToken data = arguments.getMessage ();
			final String token = data.getToken ();
			if (token != null) {
				this.logger.info ("UserCloudlet received authentication token `{}`.", token);
			} else {
				this.logger.error ("UserCloudlet did not receive authentication token.");
			}
			context.consumer.acknowledge (arguments.getToken ());
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final UserCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("UserCloudlet consumer destroyed successfully.");
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final UserCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("UserCloudlet consumer initialized successfully.");
			return Callback.SUCCESS;
		}
	}
	
	public static final class AmqpPublisherCallback
				extends DefaultAmqpPublisherConnectorCallback<UserCloudletContext, LoggingData, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final UserCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("UserCloudlet publisher destroyed successfully.");
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final UserCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("UserCloudlet publisher initialized successfully.");
			return Callback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
				extends DefaultCloudletCallback<UserCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final UserCloudletContext context, final CloudletCallbackArguments<UserCloudletContext> arguments) {
			this.logger.info ("UserCloudlet destroying...");
			return CallbackCompletion.createAndChained (context.consumer.destroy (), context.publisher.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final UserCloudletContext context, final CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
			this.logger.info ("UserCloudlet destroyed successfully.");
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final UserCloudletContext context, final CloudletCallbackArguments<UserCloudletContext> arguments) {
			this.logger.info ("UserCloudlet initializing...");
			context.cloudlet = arguments.getCloudlet ();
			final Configuration configuration = context.cloudlet.getConfiguration ();
			final Configuration consumerConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("consumer"));
			context.consumer = context.cloudlet.getConnectorFactory (AmqpQueueConsumerConnectorFactory.class).create (consumerConfiguration, AuthenticationToken.class, SerializedDataEncoder.create (AuthenticationToken.class), new AmqpConsumerCallback (), context);
			final Configuration publisherConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("publisher"));
			context.publisher = context.cloudlet.getConnectorFactory (AmqpQueuePublisherConnectorFactory.class).create (publisherConfiguration, LoggingData.class, SerializedDataEncoder.create (LoggingData.class), new AmqpPublisherCallback (), context);
			return CallbackCompletion.createAndChained (context.consumer.initialize (), context.publisher.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final UserCloudletContext context, final CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
			this.logger.info ("UserCloudlet initialized successfully.");
			final String user = ConfigUtils.resolveParameter (arguments.getCloudlet ().getConfiguration (), "test.user", String.class, "error");
			final String pass = ConfigUtils.resolveParameter (arguments.getCloudlet ().getConfiguration (), "test.password", String.class, "");
			final LoggingData data = new LoggingData (user, pass);
			context.publisher.publish (data, null);
			return Callback.SUCCESS;
		}
	}
	
	public static final class UserCloudletContext
	{
		CloudletController<UserCloudletContext> cloudlet;
		AmqpQueueConsumerConnector<AuthenticationToken, Void> consumer;
		AmqpQueuePublisherConnector<LoggingData, Void> publisher;
	}
}
