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

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueueConsumeMessage;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueueConsumer;
import eu.mosaic_cloud.cloudlets.resources.amqp.DefaultAmqpConsumerCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;


public class ConsumerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<ConsumerCloudletContext> {

		@Override
		public void initialize(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet is being initialized.");
			ICloudletController<ConsumerCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			DataEncoder<String> encoder = new PojoDataEncoder<String>(
					String.class);
			context.consumer = new AmqpQueueConsumer<ConsumerCloudlet.ConsumerCloudletContext, String>(
					queueConfiguration, cloudlet, String.class, encoder);

		}

		@Override
		public void initializeSucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet initialized successfully.");
			ICloudletController<ConsumerCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);

		}

		@Override
		public void destroy(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Consumer cloudlet was destroyed successfully.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<ConsumerCloudletContext, String> {

		@Override
		public void registerSucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<ConsumerCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
		}

		@Override
		public void initializeSucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
		}

		@Override
		public void destroySucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet consumer was destroyed successfully.");
			context.consumer = null;
			arguments.getCloudlet().destroy();
		}

		@Override
		public void acknowledgeSucceeded(ConsumerCloudletContext context,
				CallbackArguments<ConsumerCloudletContext> arguments) {
			context.consumer.unregister();

		}

		@Override
		public void consume(
				ConsumerCloudletContext context,
				AmqpQueueConsumeCallbackArguments<ConsumerCloudletContext, String> arguments) {

			AmqpQueueConsumeMessage<String> message = arguments.getMessage();
			String data = message.getData();
			MosaicLogger.getLogger().info(
					"ConsumerCloudlet received logging message for user "
							+ data);
			message.acknowledge();
		}

	}

	public static final class ConsumerCloudletContext {
		AmqpQueueConsumer<ConsumerCloudletContext, String> consumer;
	}
}
