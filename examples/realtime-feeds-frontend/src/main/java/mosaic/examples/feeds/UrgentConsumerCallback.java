package mosaic.examples.feeds;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

public final class UrgentConsumerCallback extends QueueConsumerCallback {

	@Override
	public void unregisterSucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		MosaicLogger.getLogger().info(
				"Urgent Index Message consumer unregistered successfully.");
		ICloudletController<IndexerCloudletState> cloudlet = arguments
				.getCloudlet();
		cloudlet.destroyResource(state.urgentConsumer, this);
	}

	@Override
	public void initializeSucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		// if resource initialized successfully then just register as a
		// consumer
		state.urgentConsumer.register();
	}

	@Override
	public void destroySucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		MosaicLogger.getLogger().info(
				"Urgent Index Message consumer was destroyed successfully.");
		state.urgentConsumer = null;
	}
}
