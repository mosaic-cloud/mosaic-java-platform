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
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;

public class UserCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<UserCloudletState> {

		@Override
		public void initialize(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			state.consumer = new AmqpQueueConsumer<UserCloudlet.UserCloudletState, AuthenticationToken>(
					configuration, cloudlet, AuthenticationToken.class);
			state.publisher = new AmqpQueuePublisher<UserCloudlet.UserCloudletState, LoggingData>(
					configuration, cloudlet, LoggingData.class);

		}

		@Override
		public void initializeSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			ICloudletController<UserCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);
			cloudlet.initializeResource(state.publisher,
					new AmqpPublisherCallback(), state);
			String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.user", String.class, "error");
			String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
					.getConfiguration(), "test.password", String.class, "");
		}

		@Override
		public void destroy(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			System.out.println("Logging cloudlet is being destroyed.");
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<UserCloudletState, AuthenticationToken> {

		@Override
		public void registerSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			state.consumerRunning = true;
		}

		@Override
		public void unregisterSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
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
				MosaicLogger.getLogger()
						.info("User cloudlet received authentication token: "
								+ token);
			} else {
				MosaicLogger.getLogger().error(
						"User cloudlet did not receive authentication token.");
			}
			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<UserCloudletState, LoggingData> {

		@Override
		public void registerSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
			state.publisherRunning = true;
		}

		@Override
		public void unregisterSucceeded(UserCloudletState state,
				CallbackArguments<UserCloudletState> arguments) {
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
