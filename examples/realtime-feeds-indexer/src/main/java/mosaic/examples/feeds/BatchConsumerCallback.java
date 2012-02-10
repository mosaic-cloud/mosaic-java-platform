package mosaic.examples.feeds;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.core.log.MosaicLogger;
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
