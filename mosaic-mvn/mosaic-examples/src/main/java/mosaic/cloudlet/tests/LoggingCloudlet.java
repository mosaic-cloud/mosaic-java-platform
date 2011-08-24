package mosaic.cloudlet.tests;

import java.util.concurrent.ExecutionException;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCloudletCallback;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeCallbackArguments;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumer;
import mosaic.cloudlet.resources.amqp.AmqpQueuePublishCallbackArguments;
import mosaic.cloudlet.resources.amqp.AmqpQueuePublisher;
import mosaic.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import mosaic.cloudlet.resources.amqp.DefaultAmqpPublisherCallback;
import mosaic.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.IKeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.KeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IResult;

public class LoggingCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<LoggingCloudletState> {

		@Override
		public void initialize(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet is being initialized.");
			ICloudletController<LoggingCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration kvConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("kvstore"));
			state.kvStore = new KeyValueAccessor<LoggingCloudletState>(
					kvConfiguration, cloudlet);
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			state.consumer = new AmqpQueueConsumer<LoggingCloudlet.LoggingCloudletState, LoggingData>(
					queueConfiguration, cloudlet, LoggingData.class);
			state.publisher = new AmqpQueuePublisher<LoggingCloudlet.LoggingCloudletState, AuthenticationToken>(
					queueConfiguration, cloudlet, AuthenticationToken.class);

		}

		@Override
		public void initializeSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet initialized successfully.");
			ICloudletController<LoggingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.kvStore, new KeyValueCallback(),
					state);
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);
			cloudlet.initializeResource(state.publisher,
					new AmqpPublisherCallback(), state);

		}

		@Override
		public void destroy(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger()
					.info("LoggingCloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet was destroyed successfully.");
			System.exit(0);
		}

	}

	public static final class KeyValueCallback extends
			DefaultKeyValueAccessorCallback<LoggingCloudletState> {
		private static int sets = 0;

		@Override
		public void initializeSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger
					.getLogger()
					.info("LoggingCloudlet - KeyValue accessor initialized successfully");
			String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.user", String.class, "error");
			String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.password", String.class, "");
			state.kvStore.set(user, pass, null);
		}

		@Override
		public void destroySucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			state.kvStore = null;
			if (state.publisher == null && state.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void setSucceeded(LoggingCloudletState state,
				KeyValueCallbackArguments<LoggingCloudletState> arguments) {
			sets++;
			MosaicLogger.getLogger().info(
					"LoggingCloudlet - KeyValue succeeded set no. " + sets);
			if (sets == 2) {
				ICloudletController<LoggingCloudletState> cloudlet = arguments
						.getCloudlet();
				cloudlet.destroyResource(state.kvStore, this);
			}
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<LoggingCloudletState, LoggingData> {

		@Override
		public void registerSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet consumer registered successfully.");
			state.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<LoggingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.consumer, this);
			state.consumerRunning = false;
		}

		@Override
		public void initializeSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			state.consumer.register();
		}

		@Override
		public void destroySucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet consumer was destroyed successfully.");
			state.consumer = null;
			if (state.publisher == null && state.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			state.consumer.unregister();

		}

		@Override
		public void consume(
				LoggingCloudletState state,
				AmqpQueueConsumeCallbackArguments<LoggingCloudletState, LoggingData> arguments) {
			AmqpQueueConsumeMessage<LoggingData> message = arguments
					.getMessage();
			LoggingData data = message.getData();
			MosaicLogger.getLogger().info(
					"LoggingCloudlet received logging message for user "
							+ data.user);
			IResult<Object> result = state.kvStore.get(data.user, null);
			Object passOb;
			String token = null;
			try {
				passOb = result.getResult();
				if (passOb instanceof String) {
					String pass = (String) passOb;
					if (pass.equals(data.password)) {
						token = ConfigUtils.resolveParameter(arguments
								.getCloudlet().getConfiguration(),
								"test.token", String.class, "token");
						state.kvStore.set(data.user, token, null);
					}
				}
				AuthenticationToken aToken = new AuthenticationToken(token);
				state.publisher.publish(aToken, null, null);
			} catch (InterruptedException e) {
				ExceptionTracer.traceHandled(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceHandled(e);
			}

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback
			extends
			DefaultAmqpPublisherCallback<LoggingCloudletState, AuthenticationToken> {

		@Override
		public void registerSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet publisher registered successfully.");
			state.publisherRunning = true;
		}

		@Override
		public void unregisterSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<LoggingCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.publisher, this);
			state.publisherRunning = false;
		}

		@Override
		public void initializeSucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			state.publisher.register();
		}

		@Override
		public void destroySucceeded(LoggingCloudletState state,
				CallbackArguments<LoggingCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"LoggingCloudlet publisher was destroyed successfully.");
			state.publisher = null;
			if (state.consumer == null && state.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				LoggingCloudletState state,
				AmqpQueuePublishCallbackArguments<LoggingCloudletState, AuthenticationToken> arguments) {
			state.publisher.unregister();
		}

	}

	public static final class LoggingCloudletState {
		AmqpQueueConsumer<LoggingCloudletState, LoggingData> consumer;
		AmqpQueuePublisher<LoggingCloudletState, AuthenticationToken> publisher;
		IKeyValueAccessor<LoggingCloudletState> kvStore;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}
