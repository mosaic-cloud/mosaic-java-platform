package mosaic.examples.feeds;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeCallbackArguments;
import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import mosaic.cloudlet.resources.amqp.DefaultAmqpConsumerCallback;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

import org.json.JSONObject;

public class QueueConsumerCallback extends
		DefaultAmqpConsumerCallback<IndexerCloudletState, JSONObject> {

	@Override
	public void registerSucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		MosaicLogger.getLogger().info(
				"Index Message consumer registered successfully.");
	}

	@Override
	public void consume(
			IndexerCloudletState state,
			AmqpQueueConsumeCallbackArguments<IndexerCloudletState, JSONObject> arguments) {
		AmqpQueueConsumeMessage<JSONObject> message = arguments.getMessage();

		IndexWorkflow.indexNewFeed(state, message);
		message.acknowledge();
	}

}