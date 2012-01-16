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
package eu.mosaic_cloud.cloudlet.tests;

import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.core.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlet.core.ICloudletController;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueueConsumer;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueuePublishCallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueuePublisher;
import eu.mosaic_cloud.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import eu.mosaic_cloud.cloudlet.resources.amqp.DefaultAmqpPublisherCallback;
import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.utils.PojoDataEncoder;

public class UserCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<UserCloudletContext> {

		@Override
		public void initialize(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info("UserCloudlet is being initialized.");
			ICloudletController<UserCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumer<UserCloudlet.UserCloudletContext, AuthenticationToken>(
					queueConfiguration, cloudlet, AuthenticationToken.class,
					new PojoDataEncoder<AuthenticationToken>(
							AuthenticationToken.class));
			context.publisher = new AmqpQueuePublisher<UserCloudlet.UserCloudletContext, LoggingData>(
					queueConfiguration, cloudlet, LoggingData.class,
					new PojoDataEncoder<LoggingData>(LoggingData.class));

		}

		@Override
		public void initializeSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet initialized successfully.");
			ICloudletController<UserCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);
		}

		@Override
		public void destroy(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info("UserCloudlet is being destroyed.");

		}

		@Override
		public void destroySucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<UserCloudletContext, AuthenticationToken> {

		@Override
		public void registerSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer registered successfully.");
			context.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<UserCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
			context.consumerRunning = false;
		}

		@Override
		public void initializeSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
		}

		@Override
		public void destroySucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer was destroyed successfully.");
			context.consumer = null;
			if (context.publisher == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			context.consumer.unregister();

		}

		@Override
		public void consume(
				UserCloudletContext context,
				AmqpQueueConsumeCallbackArguments<UserCloudletContext, AuthenticationToken> arguments) {
			AmqpQueueConsumeMessage<AuthenticationToken> message = arguments
					.getMessage();
			AuthenticationToken data = message.getData();
			String token = data.getToken();
			if (token != null) {
				MosaicLogger.getLogger().info(
						"UserCloudlet received authentication token: " + token);
			} else {
				MosaicLogger.getLogger().error(
						"UserCloudlet did not receive authentication token.");
			}
			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<UserCloudletContext, LoggingData> {

		@Override
		public void registerSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher registered successfully.");
			context.publisherRunning = true;
			String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.user", String.class, "error");
			String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.password", String.class, "");
			LoggingData data = new LoggingData(user, pass);
			context.publisher.publish(data, null, "");
		}

		@Override
		public void unregisterSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<UserCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
			context.publisherRunning = false;
		}

		@Override
		public void initializeSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
		}

		@Override
		public void destroySucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if (context.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				UserCloudletContext context,
				AmqpQueuePublishCallbackArguments<UserCloudletContext, LoggingData> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class UserCloudletContext {
		AmqpQueueConsumer<UserCloudletContext, AuthenticationToken> consumer;
		AmqpQueuePublisher<UserCloudletContext, LoggingData> publisher;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}
