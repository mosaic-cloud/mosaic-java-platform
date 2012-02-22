/*
 * #%L
 * mosaic-examples-realtime-feeds-indexer
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
package eu.mosaic_cloud.examples.realtime_feeds.indexer;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import org.json.JSONObject;

public class IndexerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<IndexerCloudletContext> {

		@Override
		public CallbackCompletion<Void> initialize(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			this.logger.info(
					"FeedIndexerCloudlet is being initialized.");
			ICloudletController<IndexerCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration metadataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("metadata"));
			DataEncoder<byte[]> nopEncoder = new NopDataEncoder();
			DataEncoder<JSONObject> jsonEncoder = new JSONDataEncoder();
			context.metadataStoreCallback = new MetadataKVCallback();
			context.metadataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(metadataConfiguration, JSONObject.class, jsonEncoder,
							context.metadataStoreCallback, context);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			context.dataStoreCallback = new DataKVCallback();
			context.dataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(dataConfiguration, byte[].class, nopEncoder,
							context.dataStoreCallback, context);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			context.timelinesStoreCallback = new TimelinesKVCallback();
			context.timelinesStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(timelinesConfiguration, JSONObject.class, jsonEncoder,
							context.itemsStoreCallback, context);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			context.itemsStoreCallback = new ItemsKVCallback();
			context.itemsStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(itemsConfiguration, JSONObject.class, jsonEncoder,
							context.timelinesStoreCallback, context);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			context.tasksStoreCallback = new TasksKVCallback();
			context.taskStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(tasksConfiguration, JSONObject.class, jsonEncoder,
							context.tasksStoreCallback, context);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			context.urgentConsumerCallback = new UrgentConsumerCallback();
			context.urgentConsumer = cloudlet.getConnectorFactory(IAmqpQueueConsumerConnectorFactory.class)
					.create(urgentQueueConfiguration, JSONObject.class, jsonEncoder,
							context.urgentConsumerCallback, context);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			context.batchConsumerCallback = new BatchConsumerCallback();
			context.batchConsumer = cloudlet.getConnectorFactory(IAmqpQueueConsumerConnectorFactory.class)
					.create(batchQueueConfiguration, JSONObject.class, jsonEncoder,
							context.batchConsumerCallback, context);
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			this.logger.info(
					"Feeds IndexerCloudlet initialized successfully.");
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroy(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			this.logger.info(
					"Feeds IndexerCloudlet is being destroyed.");
			ICloudletController<IndexerCloudletContext> cloudlet = arguments
					.getCloudlet();
			if (context.metadataStore != null) {
				context.metadataStore.destroy();
			}
			if (context.dataStore != null) {
				context.dataStore.destroy();
			}
			if (context.timelinesStore != null) {
				context.timelinesStore.destroy();
			}
			if (context.itemsStore != null) {
				context.itemsStore.destroy();
			}
			if (context.taskStore != null) {
				context.taskStore.destroy();
			}
			if (context.urgentConsumer != null) {
				context.urgentConsumer.unregister();
			}
			if (context.batchConsumer != null) {
				context.batchConsumer.unregister();
			}
			return ICallback.SUCCESS;
		}
	}

	public static final class IndexerCloudletContext {

		IAmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject> urgentConsumer;
		IAmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject> batchConsumer;
		IKvStoreConnector<IndexerCloudletContext, JSONObject> metadataStore;
		IKvStoreConnector<IndexerCloudletContext, byte[]> dataStore;
		IKvStoreConnector<IndexerCloudletContext, JSONObject> timelinesStore;
		IKvStoreConnector<IndexerCloudletContext, JSONObject> itemsStore;
		IKvStoreConnector<IndexerCloudletContext, JSONObject> taskStore;
		IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject> urgentConsumerCallback;
		IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject> batchConsumerCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject> metadataStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext, byte[]> dataStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject> timelinesStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject> itemsStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject> tasksStoreCallback;
	}

}
