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
import eu.mosaic_cloud.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import eu.mosaic_cloud.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.utils.DataEncoder;
import eu.mosaic_cloud.core.utils.PojoDataEncoder;

public class ConsumerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<ConsumerCloudletState> {

		@Override
		public void initialize(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet is being initialized.");
			ICloudletController<ConsumerCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			DataEncoder<String> encoder = new PojoDataEncoder<String>(
					String.class);
			state.consumer = new AmqpQueueConsumer<ConsumerCloudlet.ConsumerCloudletState, String>(
					queueConfiguration, cloudlet, String.class, encoder);

		}

		@Override
		public void initializeSucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet initialized successfully.");
			ICloudletController<ConsumerCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);

		}

		@Override
		public void destroy(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Consumer cloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<ConsumerCloudletState, String> {

		@Override
		public void registerSucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<ConsumerCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.consumer, this);
		}

		@Override
		public void initializeSucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			state.consumer.register();
		}

		@Override
		public void destroySucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer was destroyed successfully.");
			state.consumer = null;
			arguments.getCloudlet().destroy();
		}

		@Override
		public void acknowledgeSucceeded(ConsumerCloudletState state,
				CallbackArguments<ConsumerCloudletState> arguments) {
			state.consumer.unregister();

		}

		@Override
		public void consume(
				ConsumerCloudletState state,
				AmqpQueueConsumeCallbackArguments<ConsumerCloudletState, String> arguments) {

			AmqpQueueConsumeMessage<String> message = arguments.getMessage();
			String data = message.getData();
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet received logging message for user "
							+ data);
			message.acknowledge();
		}

	}

	public static final class ConsumerCloudletState {
		AmqpQueueConsumer<ConsumerCloudletState, String> consumer;
	}
}
