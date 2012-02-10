package mosaic.examples.feeds;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCloudletCallback;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumer;
import mosaic.cloudlet.resources.amqp.IAmqpQueueConsumerCallback;
import mosaic.cloudlet.resources.kvstore.IKeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.IKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.KeyValueAccessor;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.utils.DataEncoder;

import org.json.JSONObject;

public class IndexerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<IndexerCloudletState> {

		@Override
		public void initialize(IndexerCloudletState state,
				CallbackArguments<IndexerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"FeedIndexerCloudlet is being initialized.");
			ICloudletController<IndexerCloudletState> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration metadataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("metadata"));
			DataEncoder<byte[]> nopEncoder = new NopDataEncoder();
			DataEncoder<JSONObject> jsonEncoder = new JSONDataEncoder();
			state.metadataStore = new KeyValueAccessor<IndexerCloudletState>(
					metadataConfiguration, cloudlet, jsonEncoder);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			state.dataStore = new KeyValueAccessor<IndexerCloudletState>(
					dataConfiguration, cloudlet, nopEncoder);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			state.timelinesStore = new KeyValueAccessor<IndexerCloudletState>(
					timelinesConfiguration, cloudlet, jsonEncoder);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			state.itemsStore = new KeyValueAccessor<IndexerCloudletState>(
					itemsConfiguration, cloudlet, jsonEncoder);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			state.taskStore = new KeyValueAccessor<IndexerCloudletState>(
					tasksConfiguration, cloudlet, jsonEncoder);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			state.urgentConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletState, JSONObject>(
					urgentQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			state.batchConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletState, JSONObject>(
					batchQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);

		}

		@Override
		public void initializeSucceeded(IndexerCloudletState state,
				CallbackArguments<IndexerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Feeds IndexerCloudlet initialized successfully.");
			ICloudletController<IndexerCloudletState> cloudlet = arguments
					.getCloudlet();

			state.metadataStoreCallback = new MetadataKVCallback();
			cloudlet.initializeResource(state.metadataStore,
					state.metadataStoreCallback, state);

			state.dataStoreCallback = new DataKVCallback();
			cloudlet.initializeResource(state.dataStore,
					state.dataStoreCallback, state);

			state.timelinesStoreCallback = new TimelinesKVCallback();
			cloudlet.initializeResource(state.timelinesStore,
					state.timelinesStoreCallback, state);

			state.itemsStoreCallback = new ItemsKVCallback();
			cloudlet.initializeResource(state.itemsStore,
					state.itemsStoreCallback, state);

			state.tasksStoreCallback = new TasksKVCallback();
			cloudlet.initializeResource(state.taskStore,
					state.tasksStoreCallback, state);

			state.urgentConsumerCallback = new UrgentConsumerCallback();
			cloudlet.initializeResource(state.urgentConsumer,
					state.urgentConsumerCallback, state);

			state.batchConsumerCallback = new BatchConsumerCallback();
			cloudlet.initializeResource(state.batchConsumer,
					state.batchConsumerCallback, state);
		}

		@Override
		public void destroy(IndexerCloudletState state,
				CallbackArguments<IndexerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Feeds IndexerCloudlet is being destroyed.");
			ICloudletController<IndexerCloudletState> cloudlet = arguments
					.getCloudlet();
			if (state.metadataStore != null) {
				cloudlet.destroyResource(state.metadataStore,
						state.metadataStoreCallback);
			}
			if (state.dataStore != null) {
				cloudlet.destroyResource(state.dataStore,
						state.dataStoreCallback);
			}
			if (state.timelinesStore != null) {
				cloudlet.destroyResource(state.timelinesStore,
						state.timelinesStoreCallback);
			}
			if (state.itemsStore != null) {
				cloudlet.destroyResource(state.itemsStore,
						state.itemsStoreCallback);
			}
			if (state.taskStore != null) {
				cloudlet.destroyResource(state.taskStore,
						state.tasksStoreCallback);
			}
			if (state.urgentConsumer != null) {
				state.urgentConsumer.unregister();
			}
			if (state.batchConsumer != null) {
				state.batchConsumer.unregister();
			}
		}
	}

	public static final class IndexerCloudletState {

		AmqpQueueConsumer<IndexerCloudletState, JSONObject> urgentConsumer;
		AmqpQueueConsumer<IndexerCloudletState, JSONObject> batchConsumer;
		IKeyValueAccessor<IndexerCloudletState> metadataStore;
		IKeyValueAccessor<IndexerCloudletState> dataStore;
		IKeyValueAccessor<IndexerCloudletState> timelinesStore;
		IKeyValueAccessor<IndexerCloudletState> itemsStore;
		IKeyValueAccessor<IndexerCloudletState> taskStore;
		IAmqpQueueConsumerCallback<IndexerCloudletState, JSONObject> urgentConsumerCallback;
		IAmqpQueueConsumerCallback<IndexerCloudletState, JSONObject> batchConsumerCallback;
		IKeyValueAccessorCallback<IndexerCloudletState> metadataStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletState> dataStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletState> timelinesStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletState> itemsStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletState> tasksStoreCallback;
	}

}
