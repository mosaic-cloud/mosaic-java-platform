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

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.examples.realtime_feeds.indexer.IndexerCloudlet.IndexerCloudletContext;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;

public final class UrgentConsumerCallback extends QueueConsumerCallback {

	@Override
	public void unregisterSucceeded(IndexerCloudletContext context,
			CallbackArguments<IndexerCloudletContext> arguments) {
		MosaicLogger.getLogger().info(
				"Urgent Index Message consumer unregistered successfully.");
		ICloudletController<IndexerCloudletContext> cloudlet = arguments
				.getCloudlet();
		cloudlet.destroyResource(context.urgentConsumer, this);
	}

	@Override
	public void initializeSucceeded(IndexerCloudletContext context,
			CallbackArguments<IndexerCloudletContext> arguments) {
		// if resource initialized successfully then just register as a
		// consumer
		context.urgentConsumer.register();
	}

	@Override
	public void destroySucceeded(IndexerCloudletContext context,
			CallbackArguments<IndexerCloudletContext> arguments) {
		MosaicLogger.getLogger().info(
				"Urgent Index Message consumer was destroyed successfully.");
		context.urgentConsumer = null;
	}
}
