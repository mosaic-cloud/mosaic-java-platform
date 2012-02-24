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

import java.util.UUID;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import org.json.JSONObject;

public class IndexerCloudlet {

    public static final class IndexerCloudletContext {

        IAmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject, Void> urgentConsumer;

        IAmqpQueueConsumerConnector<IndexerCloudletContext, JSONObject, Void> batchConsumer;

        IKvStoreConnector<IndexerCloudletContext, JSONObject, UUID> metadataStore;

        IKvStoreConnector<IndexerCloudletContext, byte[], UUID> dataStore;

        IKvStoreConnector<IndexerCloudletContext, JSONObject, Void> timelinesStore;

        IKvStoreConnector<IndexerCloudletContext, JSONObject, UUID> itemsStore;

        IKvStoreConnector<IndexerCloudletContext, JSONObject, Void> tasksStore;

        IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject, Void> urgentConsumerCallback;

        IAmqpQueueConsumerConnectorCallback<IndexerCloudletContext, JSONObject, Void> batchConsumerCallback;

        IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, UUID> metadataStoreCallback;

        IKvStoreConnectorCallback<IndexerCloudletContext, byte[], UUID> dataStoreCallback;

        IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, UUID> timelinesStoreCallback;

        IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, Void> itemsStoreCallback;

        IKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, Void> tasksStoreCallback;
    }

    public static final class LifeCycleHandler extends
            DefaultCloudletCallback<IndexerCloudletContext> {

        @Override
        public CallbackCompletion<Void> destroy(IndexerCloudletContext context,
                CloudletCallbackArguments<IndexerCloudletContext> arguments) {
            this.logger.info("Feeds IndexerCloudlet is being destroyed.");
            final ICloudletController<IndexerCloudletContext> cloudlet = arguments.getCloudlet();
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
            if (context.tasksStore != null) {
                context.tasksStore.destroy();
            }
            if (context.urgentConsumer != null) {
                context.urgentConsumer.destroy();
            }
            if (context.batchConsumer != null) {
                context.batchConsumer.destroy();
            }
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initialize(IndexerCloudletContext context,
                CloudletCallbackArguments<IndexerCloudletContext> arguments) {
            this.logger.info("FeedIndexerCloudlet is being initialized.");
            final ICloudletController<IndexerCloudletContext> cloudlet = arguments.getCloudlet();
            final IConfiguration configuration = cloudlet.getConfiguration();
            final IConfiguration metadataConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("metadata"));
            final DataEncoder<byte[]> nopEncoder = new NopDataEncoder();
            final DataEncoder<JSONObject> jsonEncoder = new JSONDataEncoder();
            context.metadataStoreCallback = new MetadataKVCallback();
            context.metadataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
                    .create(metadataConfiguration, JSONObject.class, jsonEncoder,
                            context.metadataStoreCallback, context);
            final IConfiguration dataConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("data"));
            context.dataStoreCallback = new DataKVCallback();
            context.dataStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
                    .create(dataConfiguration, byte[].class, nopEncoder, context.dataStoreCallback,
                            context);
            final IConfiguration timelinesConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("timelines"));
            context.timelinesStoreCallback = new TimelinesKVCallback();
            context.timelinesStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
                    .create(timelinesConfiguration, JSONObject.class, jsonEncoder,
                            context.itemsStoreCallback, context);
            final IConfiguration itemsConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("items"));
            context.itemsStoreCallback = new ItemsKVCallback();
            context.itemsStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
                    .create(itemsConfiguration, JSONObject.class, jsonEncoder,
                            context.timelinesStoreCallback, context);
            final IConfiguration tasksConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("tasks"));
            context.tasksStoreCallback = new TasksKVCallback();
            context.tasksStore = cloudlet.getConnectorFactory(IKvStoreConnectorFactory.class)
                    .create(tasksConfiguration, JSONObject.class, jsonEncoder,
                            context.tasksStoreCallback, context);
            final IConfiguration urgentQueueConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("urgent.queue"));
            context.urgentConsumerCallback = new UrgentConsumerCallback();
            context.urgentConsumer = cloudlet.getConnectorFactory(
                    IAmqpQueueConsumerConnectorFactory.class).create(urgentQueueConfiguration,
                    JSONObject.class, jsonEncoder, context.urgentConsumerCallback, context);
            final IConfiguration batchQueueConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("batch.queue"));
            context.batchConsumerCallback = new BatchConsumerCallback();
            context.batchConsumer = cloudlet.getConnectorFactory(
                    IAmqpQueueConsumerConnectorFactory.class).create(batchQueueConfiguration,
                    JSONObject.class, jsonEncoder, context.batchConsumerCallback, context);
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(IndexerCloudletContext context,
                CloudletCallbackCompletionArguments<IndexerCloudletContext> arguments) {
            this.logger.info("Feeds IndexerCloudlet initialized successfully.");
            return ICallback.SUCCESS;
        }
    }
}
