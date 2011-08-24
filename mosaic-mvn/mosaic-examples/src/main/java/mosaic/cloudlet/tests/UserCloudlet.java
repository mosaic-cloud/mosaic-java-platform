package mosaic.cloudlet.tests;

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
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;

public class UserCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<UserCloudletState> {

		@Override
		public void initialize(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info("UserCloudlet is being initialized.");
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			state.consumer = new AmqpQueueConsumer<UserCloudlet.UserCloudletState, AuthenticationToken>(
					queueConfiguration, cloudlet, AuthenticationToken.class);
			state.publisher = new AmqpQueuePublisher<UserCloudlet.UserCloudletState, LoggingData>(
					queueConfiguration, cloudlet, LoggingData.class);

		}

		@Override
		public void initializeSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet initialized successfully.");
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);
			cloudlet.initializeResource(state.publisher,
					new AmqpPublisherCallback(), state);
		}

		@Override
		public void destroy(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info("UserCloudlet is being destroyed.");

		}

		@Override
		public void destroySucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet was destroyed successfully.");
			System.exit(0);
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<UserCloudletState, AuthenticationToken> {

		@Override
		public void registerSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer registered successfully.");
			state.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.consumer, this);
			state.consumerRunning = false;
		}

		@Override
		public void initializeSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			state.consumer.register();
		}

		@Override
		public void destroySucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet consumer was destroyed successfully.");
			state.consumer = null;
			if (state.publisher == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			state.consumer.unregister();

		}

		@Override
		public void consume(
				UserCloudletState state,
				AmqpQueueConsumeCallbackArguments<UserCloudletState, AuthenticationToken> arguments) {
			AmqpQueueConsumeMessage<AuthenticationToken> message = arguments
					.getMessage();
			AuthenticationToken data = message.getData();
			String token = data.getToken();
			if (token != null) {
				MosaicLogger.getLogger().info(
						"UserCloudlet received authentication token: " + token);
			} else {
				MosaicLogger.getLogger().error(
						"UserCloudlet did not receive authentication token.");
			}
			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<UserCloudletState, LoggingData> {

		@Override
		public void registerSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher registered successfully.");
			state.publisherRunning = true;
			String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.user", String.class, "error");
			String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.password", String.class, "");
			LoggingData data = new LoggingData(user, pass);
			state.publisher.publish(data, null, null);
		}

		@Override
		public void unregisterSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.publisher, this);
			state.publisherRunning = false;
		}

		@Override
		public void initializeSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			state.publisher.register();
		}

		@Override
		public void destroySucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"UserCloudlet publisher was destroyed successfully.");
			state.publisher = null;
			if (state.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				UserCloudletState state,
				AmqpQueuePublishCallbackArguments<UserCloudletState, LoggingData> arguments) {
			state.publisher.unregister();
		}

	}

	public static final class UserCloudletState {
		AmqpQueueConsumer<UserCloudletState, AuthenticationToken> consumer;
		AmqpQueuePublisher<UserCloudletState, LoggingData> publisher;
		boolean publisherRunning = false;
		boolean consumerRunning = false;
	}
}
