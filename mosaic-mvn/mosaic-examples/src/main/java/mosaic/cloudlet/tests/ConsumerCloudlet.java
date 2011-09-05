package mosaic.cloudlet.tests;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCloudletCallback;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeCallbackArguments;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumer;
import mosaic.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.utils.DataEncoder;
import mosaic.core.utils.PojoDataEncoder;

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
