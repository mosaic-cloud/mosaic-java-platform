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

import org.json.JSONObject;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueueConsumer;
import eu.mosaic_cloud.cloudlets.resources.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.cloudlets.resources.kvstore.IKeyValueAccessor;
import eu.mosaic_cloud.cloudlets.resources.kvstore.IKeyValueAccessorCallback;
import eu.mosaic_cloud.cloudlets.resources.kvstore.KeyValueAccessor;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

public class IndexerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<IndexerCloudletContext> {

		@Override
		public void initialize(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
					"FeedIndexerCloudlet is being initialized.");
			ICloudletController<IndexerCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration metadataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("metadata"));
			DataEncoder<byte[]> nopEncoder = new NopDataEncoder();
			DataEncoder<JSONObject> jsonEncoder = new JSONDataEncoder();
			context.metadataStore = new KeyValueAccessor<IndexerCloudletContext>(
					metadataConfiguration, cloudlet, jsonEncoder);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			context.dataStore = new KeyValueAccessor<IndexerCloudletContext>(
					dataConfiguration, cloudlet, nopEncoder);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			context.timelinesStore = new KeyValueAccessor<IndexerCloudletContext>(
					timelinesConfiguration, cloudlet, jsonEncoder);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			context.itemsStore = new KeyValueAccessor<IndexerCloudletContext>(
					itemsConfiguration, cloudlet, jsonEncoder);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			context.taskStore = new KeyValueAccessor<IndexerCloudletContext>(
					tasksConfiguration, cloudlet, jsonEncoder);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			context.urgentConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletContext, JSONObject>(
					urgentQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			context.batchConsumer = new AmqpQueueConsumer<IndexerCloudlet.IndexerCloudletContext, JSONObject>(
					batchQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);

		}

		@Override
		public void initializeSucceeded(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
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
		}

		@Override
		public void destroy(IndexerCloudletContext context,
				CallbackArguments<IndexerCloudletContext> arguments) {
			MosaicLogger.getLogger().info(
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
		}
	}

	public static final class IndexerCloudletContext {

		AmqpQueueConsumer<IndexerCloudletContext, JSONObject> urgentConsumer;
		AmqpQueueConsumer<IndexerCloudletContext, JSONObject> batchConsumer;
		IKeyValueAccessor<IndexerCloudletContext> metadataStore;
		IKeyValueAccessor<IndexerCloudletContext> dataStore;
		IKeyValueAccessor<IndexerCloudletContext> timelinesStore;
		IKeyValueAccessor<IndexerCloudletContext> itemsStore;
		IKeyValueAccessor<IndexerCloudletContext> taskStore;
		IAmqpQueueConsumerCallback<IndexerCloudletContext, JSONObject> urgentConsumerCallback;
		IAmqpQueueConsumerCallback<IndexerCloudletContext, JSONObject> batchConsumerCallback;
		IKeyValueAccessorCallback<IndexerCloudletContext> metadataStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletContext> dataStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletContext> timelinesStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletContext> itemsStoreCallback;
		IKeyValueAccessorCallback<IndexerCloudletContext> tasksStoreCallback;
	}

}
