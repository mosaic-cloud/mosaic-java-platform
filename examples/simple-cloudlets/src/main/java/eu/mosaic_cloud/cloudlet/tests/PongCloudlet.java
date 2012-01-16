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
import eu.mosaic_cloud.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import eu.mosaic_cloud.cloudlet.resources.kvstore.IKeyValueAccessor;
import eu.mosaic_cloud.cloudlet.resources.kvstore.KeyValueAccessor;
import eu.mosaic_cloud.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import eu.mosaic_cloud.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.utils.JsonDataEncoder;

public class PongCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PongCloudletContext> {

		@Override
		public void initialize(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger()
					.info("Pong Cloudlet is being initialized.");
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration kvConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("kvstore"));
			context.kvStore = new KeyValueAccessor<PongCloudletContext>(
					kvConfiguration, cloudlet,
					new JsonDataEncoder<PingPongData>(PingPongData.class));
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumer<PongCloudlet.PongCloudletContext, PingMessage>(
					queueConfiguration, cloudlet, PingMessage.class,
					new JsonDataEncoder<PingMessage>(PingMessage.class));
			context.publisher = new AmqpQueuePublisher<PongCloudlet.PongCloudletContext, PongMessage>(
					queueConfiguration, cloudlet, PongMessage.class,
					new JsonDataEncoder<PongMessage>(PongMessage.class));

		}

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet initialized successfully.");
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.kvStore, new KeyValueCallback(),
					context);
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);

		}

		@Override
		public void destroy(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info("Pong Cloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet was destroyed successfully.");
		}

	}

	public static final class KeyValueCallback extends
			DefaultKeyValueAccessorCallback<PongCloudletContext> {

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger
					.getLogger()
					.info("Pong Cloudlet - KeyValue accessor initialized successfully");
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			context.kvStore = null;
			if (context.publisher == null && context.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void getSucceeded(PongCloudletContext context,
				KeyValueCallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet - key value fetch data succeeded");

			// send reply to Ping Cloudlet
			PongMessage pong = new PongMessage(arguments.getKey(),
					(PingPongData) arguments.getValue());
			context.publisher.publish(pong, null, "");

			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			try {
				cloudlet.destroyResource(context.kvStore, this);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
			}
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<PongCloudletContext, PingMessage> {

		@Override
		public void registerSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
		}

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer was destroyed successfully.");
			if (context.publisher == null && context.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			context.consumer.unregister();

		}

		@Override
		public void consume(
				PongCloudletContext context,
				AmqpQueueConsumeCallbackArguments<PongCloudletContext, PingMessage> arguments) {
			AmqpQueueConsumeMessage<PingMessage> message = arguments
					.getMessage();

			// retrieve message data
			PingMessage data = message.getData();
			MosaicLogger.getLogger().info(
					"Pong Cloudlet received fetch request for key "
							+ data.getKey());

			// get value from key value store
			context.kvStore.get(data.getKey(), null);

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<PongCloudletContext, PongMessage> {

		@Override
		public void registerSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
		}

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if (context.consumer == null && context.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				PongCloudletContext context,
				AmqpQueuePublishCallbackArguments<PongCloudletContext, PongMessage> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class PongCloudletContext {

		AmqpQueueConsumer<PongCloudletContext, PingMessage> consumer;
		AmqpQueuePublisher<PongCloudletContext, PongMessage> publisher;
		IKeyValueAccessor<PongCloudletContext> kvStore;
	}
}
