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

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorCallback;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreConnector;

import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import org.json.JSONObject;

public class IndexerCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<IndexerCloudletContext> {

		@Override
		public void initialize(IndexerCloudletContext context,
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
			context.metadataStore = new KvStoreConnector<IndexerCloudletContext>(
					metadataConfiguration, cloudlet, jsonEncoder);
			IConfiguration dataConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("data"));
			context.dataStore = new KvStoreConnector<IndexerCloudletContext>(
					dataConfiguration, cloudlet, nopEncoder);
			IConfiguration timelinesConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("timelines"));
			context.timelinesStore = new KvStoreConnector<IndexerCloudletContext>(
					timelinesConfiguration, cloudlet, jsonEncoder);
			IConfiguration itemsConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("items"));
			context.itemsStore = new KvStoreConnector<IndexerCloudletContext>(
					itemsConfiguration, cloudlet, jsonEncoder);
			IConfiguration tasksConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("tasks"));
			context.taskStore = new KvStoreConnector<IndexerCloudletContext>(
					tasksConfiguration, cloudlet, jsonEncoder);

			IConfiguration urgentQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("urgent.queue"));
			context.urgentConsumer = new AmqpQueueConsumerConnector<IndexerCloudlet.IndexerCloudletContext, JSONObject>(
					urgentQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);
			IConfiguration batchQueueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("batch.queue"));
			context.batchConsumer = new AmqpQueueConsumerConnector<IndexerCloudlet.IndexerCloudletContext, JSONObject>(
					batchQueueConfiguration, cloudlet, JSONObject.class,
					jsonEncoder);

		}

		@Override
		public void initializeSucceeded(IndexerCloudletContext context,
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
		}

		@Override
		public void destroy(IndexerCloudletContext context,
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
		}
	}

	public static final class IndexerCloudletContext {

		AmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject> urgentConsumer;
		AmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject> batchConsumer;
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