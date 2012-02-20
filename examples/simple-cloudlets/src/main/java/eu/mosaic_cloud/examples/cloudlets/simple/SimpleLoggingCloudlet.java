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
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublishCallbackArguments;
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

public class SimpleLoggingCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<LoggingCloudletContext> {

		@Override
		public void initialize(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet is being initialized.");
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumerConnector<SimpleLoggingCloudlet.LoggingCloudletContext, LoggingData>(
					queueConfiguration, cloudlet, LoggingData.class,
					new PojoDataEncoder<LoggingData>(LoggingData.class));
			context.publisher = new AmqpQueuePublisherConnector<SimpleLoggingCloudlet.LoggingCloudletContext, AuthenticationToken>(
					queueConfiguration, cloudlet, AuthenticationToken.class,
					new PojoDataEncoder<AuthenticationToken>(
							AuthenticationToken.class));

		}

		@Override
		public void initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet initialized successfully.");
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);

		}

		@Override
		public void destroy(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger
					.info("LoggingCloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpQueueConsumerConnectorCallback<LoggingCloudletContext, LoggingData> {

		@Override
		public void registerSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer registered successfully.");
			context.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
			context.consumerRunning = false;
		}

		@Override
		public void initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
		}

		@Override
		public void destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer was destroyed successfully.");
			context.consumer = null;
			if (context.publisher == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			context.consumer.unregister();

		}

		@Override
		public void consume(
				LoggingCloudletContext context,
				AmqpQueueConsumeCallbackArguments<LoggingCloudletContext, LoggingData> arguments) {
			AmqpQueueConsumeMessage<LoggingData> message = arguments
					.getMessage();
			LoggingData data = message.getData();
			this.logger.info(
					"LoggingCloudlet received logging message for user "
							+ data.user);

			String token = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.token", String.class, "error");
			AuthenticationToken aToken = new AuthenticationToken(token);
			context.publisher.publish(aToken, null, "");

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback
			extends
			DefaultAmqpPublisherConnectorCallback<LoggingCloudletContext, AuthenticationToken> {

		@Override
		public void registerSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher registered successfully.");
			context.publisherRunning = true;
		}

		@Override
		public void unregisterSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
			context.publisherRunning = false;
		}

		@Override
		public void initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
		}

		@Override
		public void destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if (context.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				LoggingCloudletContext context,
				AmqpQueuePublishCallbackArguments<LoggingCloudletContext, AuthenticationToken> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class LoggingCloudletContext {

		AmqpQueueConsumerConnector<LoggingCloudletContext, LoggingData> consumer;
		AmqpQueuePublisherConnector<LoggingCloudletContext, AuthenticationToken> publisher;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}