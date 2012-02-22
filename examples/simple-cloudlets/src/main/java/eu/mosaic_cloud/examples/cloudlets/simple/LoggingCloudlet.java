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

import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeMessage;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublishCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultKvStoreConnectorCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class LoggingCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<LoggingCloudletContext> {

		@Override
		public CallbackCompletion<Void> initialize(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet is being initialized.");
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration kvConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("kvstore"));
			context.kvStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(kvConfiguration, String.class,
							new PojoDataEncoder<String>(String.class));
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.consumer = cloudlet.getConnectorFactory(IAmqpQueueConsumerConnectorFactory.class)
					.create(queueConfiguration, LoggingData.class,
							new PojoDataEncoder<LoggingData>(LoggingData.class));
			context.publisher = cloudlet.getConnectorFactory(IAmqpQueuePublisherConnectorFactory.class)
					.create (queueConfiguration, AuthenticationToken.class,
							new PojoDataEncoder<AuthenticationToken>(AuthenticationToken.class));
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet initialized successfully.");
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.kvStore,
					new KeyValueCallback(), context);
			cloudlet.initializeResource(context.consumer,
					new AmqpConsumerCallback(), context);
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroy(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger
					.info("LoggingCloudlet is being destroyed.");
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet was destroyed successfully.");
			return ICallback.SUCCESS;
		}

	}

	public static final class KeyValueCallback extends
			DefaultKvStoreConnectorCallback<LoggingCloudletContext, String> {

		private static int sets = 0;

		@Override
		public CallbackCompletion<Void> initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger
					.info("LoggingCloudlet - KeyValue accessor initialized successfully");
			String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.user", String.class, "error");
			String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.password", String.class, "");
			context.kvStore.set(user, pass, null);
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			context.kvStore = null;
			if ((context.publisher == null) && (context.consumer == null)) {
				arguments.getCloudlet().destroy();
			}
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> setSucceeded(LoggingCloudletContext context,
				KvStoreCallbackCompletionArguments<LoggingCloudletContext, String> arguments) {
			KeyValueCallback.sets++;
			this.logger.info(
					"LoggingCloudlet - KeyValue succeeded set no. "
							+ KeyValueCallback.sets);
			if (KeyValueCallback.sets == 2) {
				ICloudletController<LoggingCloudletContext> cloudlet = arguments
						.getCloudlet();
				try {
					cloudlet.destroyResource(context.kvStore, this);
				} catch (Exception e) {
					ExceptionTracer.traceIgnored(e);
					this.logger.error(
							"LoggingCloudlet.KeyValueCallback.setSucceeded() - caught exception "
									+ e.getClass().getCanonicalName());
				}
			}
			return ICallback.SUCCESS;
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpQueueConsumerConnectorCallback<LoggingCloudletContext, LoggingData> {

		@Override
		public CallbackCompletion<Void> registerSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer registered successfully.");
			context.consumerRunning = true;
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> unregisterSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.consumer, this);
			context.consumerRunning = false;
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			context.consumer.register();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet consumer was destroyed successfully.");
			context.consumer = null;
			if ((context.publisher == null) && (context.kvStore == null)) {
				arguments.getCloudlet().destroy();
			}
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> acknowledgeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			context.consumer.unregister();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> consume(
				LoggingCloudletContext context,
				AmqpQueueConsumeCallbackArguments<LoggingCloudletContext, LoggingData> arguments) {
			AmqpQueueConsumeMessage<LoggingData> message = arguments
					.getMessage();
			LoggingData data = message.getData();
			this.logger.info(
					"LoggingCloudlet received logging message for user "
							+ data.user);
			IResult<String> result = context.kvStore.get(data.user, null);
			String passOb;
			String token = null;
			try {
				passOb = result.getResult();
				if (passOb instanceof String) {
					String pass = (String) passOb;
					if (pass.equals(data.password)) {
						token = ConfigUtils.resolveParameter(arguments
								.getCloudlet().getConfiguration(),
								"test.token", String.class, "token");
						context.kvStore.set(data.user, token, null);
					}
				}
				AuthenticationToken aToken = new AuthenticationToken(token);
				context.publisher.publish(aToken, null, "");
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceIgnored(e);
			}
			message.acknowledge();
			return ICallback.SUCCESS;
		}

	}

	public static final class AmqpPublisherCallback
			extends
			DefaultAmqpPublisherConnectorCallback<LoggingCloudletContext, AuthenticationToken> {

		@Override
		public CallbackCompletion<Void> registerSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher registered successfully.");
			context.publisherRunning = true;
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> unregisterSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<LoggingCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
			context.publisherRunning = false;
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(LoggingCloudletContext context,
				CallbackArguments<LoggingCloudletContext> arguments) {
			this.logger.info(
					"LoggingCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			if ((context.consumer == null) && (context.kvStore == null)) {
				arguments.getCloudlet().destroy();
			}
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> publishSucceeded(
				LoggingCloudletContext context,
				AmqpQueuePublishCallbackCompletionArguments<LoggingCloudletContext, AuthenticationToken> arguments) {
			context.publisher.unregister();
			return ICallback.SUCCESS;
		}

	}

	public static final class LoggingCloudletContext {

		IAmqpQueueConsumerConnector<LoggingCloudletContext, LoggingData> consumer;
		IAmqpQueuePublisherConnector<LoggingCloudletContext, AuthenticationToken> publisher;
		IKvStoreConnector<LoggingCloudletContext, String> kvStore;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}
