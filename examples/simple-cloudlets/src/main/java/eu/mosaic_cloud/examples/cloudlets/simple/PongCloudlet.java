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

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;

import eu.mosaic_cloud.cloudlets.tools.DefaultKvStoreConnectorCallback;

import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;

import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.utils.JsonDataEncoder;

public class PongCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PongCloudletContext> {

		@Override
		public void initialize(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet is being initialized.");
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration kvConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("kvstore"));
			context.kvStore = new KvStoreConnector<PongCloudletContext>(
					kvConfiguration, cloudlet,
					new JsonDataEncoder<PingPongData>(PingPongData.class));
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = new AmqpQueueConsumerConnector<PongCloudlet.PongCloudletContext, PingMessage>(
					queueConfiguration, cloudlet, PingMessage.class,
					new JsonDataEncoder<PingMessage>(PingMessage.class));
			context.publisher = new AmqpQueuePublisherConnector<PongCloudlet.PongCloudletContext, PongMessage>(
					queueConfiguration, cloudlet, PongMessage.class,
					new JsonDataEncoder<PongMessage>(PongMessage.class));

		}

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet initialized successfully.");
			ICloudletController<PongCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.kvStore,
					new KeyValueCallback(), context);
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);

		}

		@Override
		public void destroy(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet was destroyed successfully.");
		}

	}

	public static final class KeyValueCallback extends
			DefaultKvStoreConnectorCallback<PongCloudletContext> {

		@Override
		public void initializeSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger
					.info("Pong Cloudlet - KeyValue accessor initialized successfully");
		}

		@Override
		public void destroySucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			context.kvStore = null;
			if ((context.publisher == null) && (context.consumer == null)) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void getSucceeded(PongCloudletContext context,
				KvStoreCallbackCompletionArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet - key value fetch data succeeded");

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
			DefaultAmqpQueueConsumerConnectorCallback<PongCloudletContext, PingMessage> {

		@Override
		public void registerSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger.info("Pong Cloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger
					.info("Pong Cloudlet consumer unregistered successfully.");
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
			this.logger
					.info("Pong Cloudlet consumer was destroyed successfully.");
			if ((context.publisher == null) && (context.kvStore == null)) {
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
			this.logger.info("Pong Cloudlet received fetch request for key "
					+ data.getKey());

			// get value from key value store
			context.kvStore.get(data.getKey(), null);

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherConnectorCallback<PongCloudletContext, PongMessage> {

		@Override
		public void registerSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger
					.info("Pong Cloudlet publisher registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletContext context,
				CallbackArguments<PongCloudletContext> arguments) {
			this.logger
					.info("Pong Cloudlet publisher unregistered successfully.");
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
			this.logger
					.info("Pong Cloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if ((context.consumer == null) && (context.kvStore == null)) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				PongCloudletContext context,
				AmqpQueuePublishCallbackCompletionArguments<PongCloudletContext, PongMessage> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class PongCloudletContext {

		AmqpQueueConsumerConnector<PongCloudletContext, PingMessage> consumer;
		AmqpQueuePublisherConnector<PongCloudletContext, PongMessage> publisher;
		IKvStoreConnector<PongCloudletContext> kvStore;
	}
}
