package mosaic.examples.feeds;

import org.json.JSONObject;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCloudletCallback;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumer;
import mosaic.cloudlet.resources.kvstore.IKeyValueAccessor;
import mosaic.cloudlet.resources.kvstore.KeyValueAccessor;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;

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
			state.metadataStore = new KeyValueAccessor<IndexerCloudletState>(
					metadataConfiguration, cloudlet);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			state.dataStore = new KeyValueAccessor<IndexerCloudletState>(
					dataConfiguration, cloudlet);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			state.timelinesStore = new KeyValueAccessor<IndexerCloudletState>(
					timelinesConfiguration, cloudlet);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			state.itemsStore = new KeyValueAccessor<IndexerCloudletState>(
					itemsConfiguration, cloudlet);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			state.taskStore = new KeyValueAccessor<IndexerCloudletState>(
					tasksConfiguration, cloudlet);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			state.urgentConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletState, JSONObject>(
					urgentQueueConfiguration, cloudlet, JSONObject.class);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			state.batchConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletState, JSONObject>(
					batchQueueConfiguration, cloudlet, JSONObject.class);

		}

		@Override
		public void initializeSucceeded(IndexerCloudletState state,
				CallbackArguments<IndexerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Feeds IndexerCloudlet initialized successfully.");
			ICloudletController<IndexerCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(state.metadataStore,
					new MetadataKVCallback(), state);
			cloudlet.initializeResource(state.dataStore, new DataKVCallback(),
					state);
			cloudlet.initializeResource(state.timelinesStore,
					new TimelinesKVCallback(), state);
			cloudlet.initializeResource(state.itemsStore,
					new ItemsKVCallback(), state);
			cloudlet.initializeResource(state.taskStore, new TasksKVCallback(),
					state);

			cloudlet.initializeResource(state.urgentConsumer,
					new UrgentConsumerCallback(), state);
			cloudlet.initializeResource(state.batchConsumer,
					new BatchConsumerCallback(), state);
		}

		@Override
		public void destroy(IndexerCloudletState state,
				CallbackArguments<IndexerCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"Feeds IndexerCloudlet is being destroyed.");

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
	}

}
