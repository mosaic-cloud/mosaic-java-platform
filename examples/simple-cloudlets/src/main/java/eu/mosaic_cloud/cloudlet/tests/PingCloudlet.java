/*
 * #%L
 * mosaic-examples
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
import eu.mosaic_cloud.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.utils.JsonDataEncoder;

public class PingCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PingCloudletState> {

		@Override
		public void initialize(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger()
					.info("Ping Cloudlet is being initialized.");
			ICloudletController<PingCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			state.consumer = new AmqpQueueConsumer<PingCloudlet.PingCloudletState, PongMessage>(
					queueConfiguration, cloudlet, PongMessage.class,
					new JsonDataEncoder<PongMessage>(PongMessage.class));
			state.publisher = new AmqpQueuePublisher<PingCloudlet.PingCloudletState, PingMessage>(
					queueConfiguration, cloudlet, PingMessage.class,
					new JsonDataEncoder<PingMessage>(PingMessage.class));

		}

		@Override
		public void initializeSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet initialized successfully.");
			ICloudletController<PingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);
			cloudlet.initializeResource(state.publisher,
					new AmqpPublisherCallback(), state);
		}

		@Override
		public void destroy(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info("Ping Cloudlet is being destroyed.");

		}

		@Override
		public void destroySucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<PingCloudletState, PongMessage> {

		@Override
		public void registerSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<PingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.consumer, this);
		}

		@Override
		public void initializeSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			state.consumer.register();
		}

		@Override
		public void destroySucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet consumer was destroyed successfully.");
			state.consumer = null;
			if (state.publisher == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			state.consumer.unregister();

		}

		@Override
		public void consume(
				PingCloudletState state,
				AmqpQueueConsumeCallbackArguments<PingCloudletState, PongMessage> arguments) {
			AmqpQueueConsumeMessage<PongMessage> message = arguments
					.getMessage();
			PongMessage data = message.getData();
			String key = data.getKey();
			PingPongData value = data.getValue();
			MosaicLogger.getLogger().info(
					"Ping Cloudlet received key-value pair: (" + key + ", "
							+ value + ")");

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<PingCloudletState, PingMessage> {

		@Override
		public void registerSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet publisher registered successfully.");
			PingMessage data = new PingMessage("pingpong");
			state.publisher.publish(data, null, "");
		}

		@Override
		public void unregisterSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.publisher, this);
		}

		@Override
		public void initializeSucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			state.publisher.register();
		}

		@Override
		public void destroySucceeded(PingCloudletState state,
				CallbackArguments<PingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Ping Cloudlet publisher was destroyed successfully.");
			state.publisher = null;
			if (state.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				PingCloudletState state,
				AmqpQueuePublishCallbackArguments<PingCloudletState, PingMessage> arguments) {
			state.publisher.unregister();
		}

	}

	public static final class PingCloudletState {

		AmqpQueueConsumer<PingCloudletState, PongMessage> consumer;
		AmqpQueuePublisher<PingCloudletState, PingMessage> publisher;
	}
}
