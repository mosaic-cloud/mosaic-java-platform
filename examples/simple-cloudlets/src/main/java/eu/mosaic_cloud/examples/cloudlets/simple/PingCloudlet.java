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
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.JsonDataEncoder;

public class PingCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PingCloudletContext> {

		@Override
		public void initialize(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger
					.info("Ping Cloudlet is being initialized.");
			ICloudletController<PingCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumerConnector<PingCloudlet.PingCloudletContext, PongMessage>(
					queueConfiguration, cloudlet, PongMessage.class,
					new JsonDataEncoder<PongMessage>(PongMessage.class));
			context.publisher = new AmqpQueuePublisherConnector<PingCloudlet.PingCloudletContext, PingMessage>(
					queueConfiguration, cloudlet, PingMessage.class,
					new JsonDataEncoder<PingMessage>(PingMessage.class));

		}

		@Override
		public void initializeSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet initialized successfully.");
			ICloudletController<PingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);
		}

		@Override
		public void destroy(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info("Ping Cloudlet is being destroyed.");

		}

		@Override
		public void destroySucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpQueueConsumerConnectorCallback<PingCloudletContext, PongMessage> {

		@Override
		public void registerSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<PingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
		}

		@Override
		public void initializeSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
		}

		@Override
		public void destroySucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet consumer was destroyed successfully.");
			context.consumer = null;
			if (context.publisher == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			context.consumer.unregister();

		}

		@Override
		public void consume(
				PingCloudletContext context,
				AmqpQueueConsumeCallbackArguments<PingCloudletContext, PongMessage> arguments) {
			AmqpQueueConsumeMessage<PongMessage> message = arguments
					.getMessage();
			PongMessage data = message.getData();
			String key = data.getKey();
			PingPongData value = data.getValue();
			this.logger.info(
					"Ping Cloudlet received key-value pair: (" + key + ", "
							+ value + ")");

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherConnectorCallback<PingCloudletContext, PingMessage> {

		@Override
		public void registerSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet publisher registered successfully.");
			PingMessage data = new PingMessage("pingpong");
			context.publisher.publish(data, null, "");
		}

		@Override
		public void unregisterSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
		}

		@Override
		public void initializeSucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
		}

		@Override
		public void destroySucceeded(PingCloudletContext context,
				CallbackArguments<PingCloudletContext> arguments) {
			this.logger.info(
					"Ping Cloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if (context.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				PingCloudletContext context,
				AmqpQueuePublishCallbackArguments<PingCloudletContext, PingMessage> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class PingCloudletContext {

		AmqpQueueConsumerConnector<PingCloudletContext, PongMessage> consumer;
		AmqpQueuePublisherConnector<PingCloudletContext, PingMessage> publisher;
	}
}
