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
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.YYY_kv_KvStoreConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.YYY_kv_KvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.YYY_amqp_AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.YYY_amqp_AmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.YYY_amqp_AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.YYY_amqp_AmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.YYY_core_Callback;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.serialization.SerializedDataEncoder;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class LoggingCloudlet
{
	public static final class AmqpConsumerCallback
				extends DefaultAmqpQueueConsumerConnectorCallback<LoggingCloudletContext, LoggingData, Void>
	{
		@Override
		public CallbackCompletion<Void> consume (final LoggingCloudletContext context, final AmqpQueueConsumeCallbackArguments<LoggingData> arguments) {
			final LoggingData data = arguments.getMessage ();
			this.logger.info ("LoggingCloudlet received logging message for user `{}`.", data.user);
			final CallbackCompletion<String> result = context.kvStore.get (data.user, null);
			String passOb;
			String token = null;
			result.await ();
			passOb = result.getOutcome ();
			final String pass = passOb;
			if (pass.equals (data.password)) {
				token = ConfigUtils.resolveParameter (arguments.getCloudlet ().getConfiguration (), "test.token", String.class, "token");
				context.kvStore.set (data.user, token, null);
			}
			final AuthenticationToken aToken = new AuthenticationToken (token);
			context.publisher.publish (aToken, null);
			context.consumer.acknowledge (arguments.getToken ());
			context.cloudlet.destroy ();
			return YYY_core_Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet consumer destroyed successfully.");
			return YYY_core_Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet consumer initialized successfully.");
			return YYY_core_Callback.SUCCESS;
		}
	}
	
	public static final class AmqpPublisherCallback
				extends DefaultAmqpPublisherConnectorCallback<LoggingCloudletContext, AuthenticationToken, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet publisher destroyed successfully.");
			return YYY_core_Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet publisher initialized successfully.");
			return YYY_core_Callback.SUCCESS;
		}
	}
	
	public static final class KeyValueCallback
				extends DefaultKvStoreConnectorCallback<LoggingCloudletContext, String, Void>
	{
		@Override
		public CallbackCompletion<Void> destroySucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet store destroyed successfully.");
			return YYY_core_Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final LoggingCloudletContext context, final CallbackArguments arguments) {
			this.logger.info ("LoggingCloudlet store initialized successfully.");
			final String user = ConfigUtils.resolveParameter (arguments.getCloudlet ().getConfiguration (), "test.user", String.class, "error");
			final String pass = ConfigUtils.resolveParameter (arguments.getCloudlet ().getConfiguration (), "test.password", String.class, "");
			context.kvStore.set (user, pass, null);
			return YYY_core_Callback.SUCCESS;
		}
	}
	
	public static final class LifeCycleHandler
				extends DefaultCloudletCallback<LoggingCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final LoggingCloudletContext context, final CloudletCallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info ("LoggingCloudlet destroying...");
			return CallbackCompletion.createAndChained (context.kvStore.destroy (), context.consumer.destroy (), context.publisher.destroy ());
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final LoggingCloudletContext context, final CloudletCallbackCompletionArguments<LoggingCloudletContext> arguments) {
			this.logger.info ("LoggingCloudlet destroyed successfully.");
			return YYY_core_Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final LoggingCloudletContext context, final CloudletCallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info ("LoggingCloudlet initializing...");
			context.cloudlet = arguments.getCloudlet ();
			final Configuration configuration = context.cloudlet.getConfiguration ();
			final Configuration kvConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("kvstore"));
			context.kvStore = context.cloudlet.getConnectorFactory (YYY_kv_KvStoreConnectorFactory.class).create (kvConfiguration, String.class, SerializedDataEncoder.create (String.class), new KeyValueCallback (), context);
			final Configuration consumerConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("consumer"));
			context.consumer = context.cloudlet.getConnectorFactory (YYY_amqp_AmqpQueueConsumerConnectorFactory.class).create (consumerConfiguration, LoggingData.class, SerializedDataEncoder.create (LoggingData.class), new AmqpConsumerCallback (), context);
			final Configuration publisherConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("publisher"));
			context.publisher = context.cloudlet.getConnectorFactory (YYY_amqp_AmqpQueuePublisherConnectorFactory.class).create (publisherConfiguration, AuthenticationToken.class, SerializedDataEncoder.create (AuthenticationToken.class), new AmqpPublisherCallback (), context);
			return CallbackCompletion.createAndChained (context.kvStore.initialize (), context.consumer.initialize (), context.publisher.initialize ());
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final LoggingCloudletContext context, final CloudletCallbackCompletionArguments<LoggingCloudletContext> arguments) {
			this.logger.info ("LoggingCloudlet initialized successfully.");
			return YYY_core_Callback.SUCCESS;
		}
	}
	
	public static final class LoggingCloudletContext
	{
		CloudletController<LoggingCloudletContext> cloudlet;
		YYY_amqp_AmqpQueueConsumerConnector<LoggingData, Void> consumer;
		YYY_kv_KvStoreConnector<String, Void> kvStore;
		YYY_amqp_AmqpQueuePublisherConnector<AuthenticationToken, Void> publisher;
	}
}
