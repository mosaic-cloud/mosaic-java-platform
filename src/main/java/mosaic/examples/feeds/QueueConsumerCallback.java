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
		// TODO if message is invalid then just ignore it and acknowledge
		// else, index data
		MosaicLogger.getLogger().trace(
				"**************Received index message: " + message);

		try {
			Thread.sleep(1000);
			IndexWorkflow.indexNewFeed(state, message);
		} catch (Throwable e) {
			e.printStackTrace();
			MosaicLogger.getLogger().error(e.getMessage());
		}
		MosaicLogger.getLogger().trace("Message consumed " + message);
	}

	// function _onIndexTaskSucceeded (_context, _url, _urlClass, _outcome) {
	// if (_outcome.items !== null) {
	// transcript.traceInformation
	// ("succeeded indexing `%s` (new items found); sending items...", _url);
	// if ((_context.itemPublisher !== undefined) &&
	// (_context.itemPublisher._ready))
	// for (var _itemIndex in _outcome.items) {
	// var _item = _outcome.items[_itemIndex];
	// _context.itemPublisher.publish (_item, _item.feed);
	// }
	// else
	// transcript.traceWarning ("failed sending item; ignoring!");
	// } else
	// transcript.traceInformation
	// ("succeeded indexing `%s` (no new items found)", _url);
	// }

}