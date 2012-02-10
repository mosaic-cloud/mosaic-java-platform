package mosaic.examples.feeds;

import java.util.HashMap;
import java.util.Map;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

public class DataKVCallback extends
		DefaultKeyValueAccessorCallback<IndexerCloudletState> {

	private static final String BUCKET_NAME = "feed-data";

	@Override
	public void destroySucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		state.dataStore = null;
	}

	@Override
	public void getSucceeded(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		String key = arguments.getKey();
		MosaicLogger.getLogger().trace(
				"succeeded fetch (" + DataKVCallback.BUCKET_NAME + "," + key
						+ ")");
		IndexWorkflow.parseLatestFeed(arguments.getValue(),
				arguments.getExtra());
	}

	@Override
	public void getFailed(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		String key = arguments.getKey();
		MosaicLogger.getLogger()
				.warn("failed fetch (" + DataKVCallback.BUCKET_NAME + "," + key
						+ ")");
		Map<String, String> errorMssg = new HashMap<String, String>(4);
		errorMssg.put("reason", "unexpected key-value store error");
		errorMssg.put("message", arguments.getValue().toString());
		errorMssg.put("bucket", DataKVCallback.BUCKET_NAME);
		errorMssg.put("key", key);
		IndexWorkflow.onIndexError(errorMssg, arguments.getExtra());
	}
}
