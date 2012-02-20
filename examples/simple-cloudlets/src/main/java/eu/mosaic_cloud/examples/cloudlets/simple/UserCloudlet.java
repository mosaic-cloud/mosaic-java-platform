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
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeMessage;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublishCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnector;

import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;

import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;

public class UserCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<UserCloudletContext> {

		@Override
		public void initialize(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info("UserCloudlet is being initialized.");
			ICloudletController<UserCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumerConnector<UserCloudlet.UserCloudletContext, AuthenticationToken>(
					queueConfiguration, cloudlet, AuthenticationToken.class,
					new PojoDataEncoder<AuthenticationToken>(
							AuthenticationToken.class));
			context.publisher = new AmqpQueuePublisherConnector<UserCloudlet.UserCloudletContext, LoggingData>(
					queueConfiguration, cloudlet, LoggingData.class,
					new PojoDataEncoder<LoggingData>(LoggingData.class));

		}

		@Override
		public void initializeSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info(
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
			this.logger.info("UserCloudlet is being destroyed.");

		}

		@Override
		public void destroySucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info(
					"UserCloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback
			extends
			DefaultAmqpQueueConsumerConnectorCallback<UserCloudletContext, AuthenticationToken> {

		@Override
		public void registerSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info(
					"UserCloudlet consumer registered successfully.");
			context.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info(
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
			this.logger.info(
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
				this.logger.info(
						"UserCloudlet received authentication token: " + token);
			} else {
				this.logger.error(
						"UserCloudlet did not receive authentication token.");
			}
			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherConnectorCallback<UserCloudletContext, LoggingData> {

		@Override
		public void registerSucceeded(UserCloudletContext context,
				CallbackArguments<UserCloudletContext> arguments) {
			this.logger.info(
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
			this.logger.info(
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
			this.logger.info(
					"UserCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if (context.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				UserCloudletContext context,
				AmqpQueuePublishCallbackCompletionArguments<UserCloudletContext, LoggingData> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class UserCloudletContext {

		AmqpQueueConsumerConnector<UserCloudletContext, AuthenticationToken> consumer;
		AmqpQueuePublisherConnector<UserCloudletContext, LoggingData> publisher;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}
