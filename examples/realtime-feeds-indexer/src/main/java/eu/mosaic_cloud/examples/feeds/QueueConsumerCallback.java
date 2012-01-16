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
package eu.mosaic_cloud.examples.feeds;

import eu.mosaic_cloud.examples.feeds.IndexerCloudlet.IndexerCloudletContext;

import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import eu.mosaic_cloud.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import eu.mosaic_cloud.core.log.MosaicLogger;
import org.json.JSONObject;

public class QueueConsumerCallback extends
		DefaultAmqpConsumerCallback<IndexerCloudletContext, JSONObject> {

	@Override
	public void registerSucceeded(IndexerCloudletContext context,
			CallbackArguments<IndexerCloudletContext> arguments) {
		MosaicLogger.getLogger().info(
				"Index Message consumer registered successfully.");
	}

	@Override
	public void consume(
			IndexerCloudletContext context,
			AmqpQueueConsumeCallbackArguments<IndexerCloudletContext, JSONObject> arguments) {
		AmqpQueueConsumeMessage<JSONObject> message = arguments.getMessage();

		IndexWorkflow.indexNewFeed(context, message);
		message.acknowledge();
	}

}