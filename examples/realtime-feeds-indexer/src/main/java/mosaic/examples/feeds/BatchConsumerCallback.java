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
package mosaic.examples.feeds;

import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.core.ICloudletController;
import eu.mosaic_cloud.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

public final class BatchConsumerCallback extends QueueConsumerCallback {

	@Override
	public void unregisterSucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		MosaicLogger.getLogger().info(
				"Batch Index Message consumer unregistered successfully.");
		ICloudletController<IndexerCloudletState> cloudlet = arguments
				.getCloudlet();
		cloudlet.destroyResource(state.batchConsumer, this);
	}

	@Override
	public void initializeSucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		// if resource initialized successfully then just register as a
		// consumer
		state.batchConsumer.register();
	}

	@Override
	public void destroySucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		MosaicLogger.getLogger().info(
				"Batch Index Message consumer was destroyed successfully.");
		state.batchConsumer = null;
	}
}
