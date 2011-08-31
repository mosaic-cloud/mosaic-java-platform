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
import mosaic.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.IKeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.KeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.utils.JsonDataEncoder;

public class PongCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PongCloudletState> {

		@Override
		public void initialize(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger()
					.info("Pong Cloudlet is being initialized.");
			ICloudletController<PongCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration kvConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("kvstore"));
			state.kvStore = new KeyValueAccessor<PongCloudletState>(
					kvConfiguration, cloudlet, new JsonDataEncoder<PingPongData>(
							PingPongData.class));
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			state.consumer = new AmqpQueueConsumer<PongCloudlet.PongCloudletState, PingMessage>(
					queueConfiguration, cloudlet, PingMessage.class,
					new JsonDataEncoder<PingMessage>(PingMessage.class));
			state.publisher = new AmqpQueuePublisher<PongCloudlet.PongCloudletState, PongMessage>(
					queueConfiguration, cloudlet, PongMessage.class,
					new JsonDataEncoder<PongMessage>(PongMessage.class));

		}

		@Override
		public void initializeSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet initialized successfully.");
			ICloudletController<PongCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.kvStore, new KeyValueCallback(),
					state);
			cloudlet.initializeResource(state.consumer,
					new AmqpConsumerCallback(), state);
			cloudlet.initializeResource(state.publisher,
					new AmqpPublisherCallback(), state);

		}

		@Override
		public void destroy(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info("Pong Cloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet was destroyed successfully.");
		}

	}

	public static final class KeyValueCallback extends
			DefaultKeyValueAccessorCallback<PongCloudletState> {

		@Override
		public void initializeSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger
					.getLogger()
					.info("Pong Cloudlet - KeyValue accessor initialized successfully");
		}

		@Override
		public void destroySucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			state.kvStore = null;
			if (state.publisher == null && state.consumer == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void getSucceeded(PongCloudletState state,
				KeyValueCallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet - key value fetch data succeeded");

			// send reply to Ping Cloudlet
			PongMessage pong = new PongMessage(arguments.getKey(),
					 (PingPongData) arguments.getValue());
			state.publisher.publish(pong, null, "");

			ICloudletController<PongCloudletState> cloudlet = arguments
					.getCloudlet();
			try {
				cloudlet.destroyResource(state.kvStore, this);
			} catch (Exception e) {
				ExceptionTracer.traceDeferred(e);
			}
		}

	}

	public static final class AmqpConsumerCallback extends
			DefaultAmqpConsumerCallback<PongCloudletState, PingMessage> {

		@Override
		public void registerSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer unregistered successfully.");
			// if unregistered as consumer is successful then destroy resource
			ICloudletController<PongCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.consumer, this);
		}

		@Override
		public void initializeSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// consumer
			state.consumer.register();
		}

		@Override
		public void destroySucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet consumer was destroyed successfully.");
			if (state.publisher == null && state.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void acknowledgeSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			state.consumer.unregister();

		}

		@Override
		public void consume(
				PongCloudletState state,
				AmqpQueueConsumeCallbackArguments<PongCloudletState, PingMessage> arguments) {
			AmqpQueueConsumeMessage<PingMessage> message = arguments
					.getMessage();

			// retrieve message data
			PingMessage data = message.getData();
			MosaicLogger.getLogger().info(
					"Pong Cloudlet received fetch request for key "
							+ data.getKey());

			// get value from key value store
			state.kvStore.get(data.getKey(), null);

			message.acknowledge();
		}

	}

	public static final class AmqpPublisherCallback extends
			DefaultAmqpPublisherCallback<PongCloudletState, PongMessage> {

		@Override
		public void registerSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher registered successfully.");
		}

		@Override
		public void unregisterSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PongCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(state.publisher, this);
		}

		@Override
		public void initializeSucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			state.publisher.register();
		}

		@Override
		public void destroySucceeded(PongCloudletState state,
				CallbackArguments<PongCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Pong Cloudlet publisher was destroyed successfully.");
			state.publisher = null;
			if (state.consumer == null && state.kvStore == null) {
				arguments.getCloudlet().destroy();
			}
		}

		@Override
		public void publishSucceeded(
				PongCloudletState state,
				AmqpQueuePublishCallbackArguments<PongCloudletState, PongMessage> arguments) {
			state.publisher.unregister();
		}

	}

	public static final class PongCloudletState {
		AmqpQueueConsumer<PongCloudletState, PingMessage> consumer;
		AmqpQueuePublisher<PongCloudletState, PongMessage> publisher;
		IKeyValueAccessor<PongCloudletState> kvStore;
	}
}
