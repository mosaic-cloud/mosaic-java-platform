package mosaic.examples.feeds;

import java.util.HashMap;
import java.util.Map;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

public class TasksKVCallback extends
		DefaultKeyValueAccessorCallback<IndexerCloudletState> {

	private static final String BUCKET_NAME = "feed-tasks";

	@Override
	public void destroySucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		state.taskStore = null;
	}

	@Override
	public void setSucceeded(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		IndexWorkflow.sendAcknowledge(arguments.getExtra());
	}

	@Override
	public void setFailed(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		handleError(arguments);
	}

	private void handleError(
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		String key = arguments.getKey();
		MosaicLogger.getLogger().warn(
				"failed fetch (" + TasksKVCallback.BUCKET_NAME + "," + key
						+ ")");
		Map<String, String> errorMssg = new HashMap<String, String>(4);
		errorMssg.put("reason", "unexpected key-value store error");
		errorMssg.put("message", arguments.getValue().toString());
		errorMssg.put("bucket", TasksKVCallback.BUCKET_NAME);
		errorMssg.put("key", key);
		IndexWorkflow.onIndexError(errorMssg, arguments.getExtra());
	}
}
