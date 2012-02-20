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
			context.metadataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(metadataConfiguration, JSONObject.class, jsonEncoder);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			context.dataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(dataConfiguration, byte[].class, nopEncoder);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			context.timelinesStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(timelinesConfiguration, JSONObject.class, jsonEncoder);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			context.itemsStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(itemsConfiguration, JSONObject.class, jsonEncoder);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			context.taskStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
					.create(tasksConfiguration, JSONObject.class, jsonEncoder);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			context.urgentConsumer = cloudlet.getConnectorFactory(IAmqpQueueConsumerConnectorFactory.class)
					.create(urgentQueueConfiguration, JSONObject.class, jsonEncoder);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			context.batchConsumer = cloudlet.getConnectorFactory(IAmqpQueueConsumerConnectorFactory.class)
					.create(batchQueueConfiguration, JSONObject.class, jsonEncoder);
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			this.logger.info(
					"Feeds IndexerCloudlet initialized successfully.");
			ICloudletController<IndexerCloudletContext> cloudlet = arguments
					.getCloudlet();

			context.metadataStoreCallback = new MetadataKVCallback();
			cloudlet.initializeResource(context.metadataStore,
					context.metadataStoreCallback, context);

			context.dataStoreCallback = new DataKVCallback();
			cloudlet.initializeResource(context.dataStore,
					context.dataStoreCallback, context);

			context.timelinesStoreCallback = new TimelinesKVCallback();
			cloudlet.initializeResource(context.timelinesStore,
					context.timelinesStoreCallback, context);

			context.itemsStoreCallback = new ItemsKVCallback();
			cloudlet.initializeResource(context.itemsStore,
					context.itemsStoreCallback, context);

			context.tasksStoreCallback = new TasksKVCallback();
			cloudlet.initializeResource(context.taskStore,
					context.tasksStoreCallback, context);

			context.urgentConsumerCallback = new UrgentConsumerCallback();
			cloudlet.initializeResource(context.urgentConsumer,
					context.urgentConsumerCallback, context);

			context.batchConsumerCallback = new BatchConsumerCallback();
			cloudlet.initializeResource(context.batchConsumer,
					context.batchConsumerCallback, context);

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
				cloudlet.destroyResource(context.metadataStore,
						context.metadataStoreCallback);
			}
			if (context.dataStore != null) {
				cloudlet.destroyResource(context.dataStore,
						context.dataStoreCallback);
			}
			if (context.timelinesStore != null) {
				cloudlet.destroyResource(context.timelinesStore,
						context.timelinesStoreCallback);
			}
			if (context.itemsStore != null) {
				cloudlet.destroyResource(context.itemsStore,
						context.itemsStoreCallback);
			}
			if (context.taskStore != null) {
				cloudlet.destroyResource(context.taskStore,
						context.tasksStoreCallback);
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
		IKvStoreConnector<IndexerCloudletContext> metadataStore;
		IKvStoreConnector<IndexerCloudletContext> dataStore;
		IKvStoreConnector<IndexerCloudletContext> timelinesStore;
		IKvStoreConnector<IndexerCloudletContext> itemsStore;
		IKvStoreConnector<IndexerCloudletContext> taskStore;
		IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject> urgentConsumerCallback;
		IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject> batchConsumerCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext> metadataStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext> dataStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext> timelinesStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext> itemsStoreCallback;
		IKvStoreConnectorCallback<IndexerCloudletContext> tasksStoreCallback;
	}

}
