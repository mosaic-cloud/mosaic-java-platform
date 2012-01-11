package mosaic.examples.feeds;

import java.util.HashMap;
import java.util.Map;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

import org.json.JSONException;
import org.json.JSONObject;

public final class MetadataKVCallback extends
		DefaultKeyValueAccessorCallback<IndexerCloudletState> {

	private static final String BUCKET_NAME = "feed-metadata";

	@Override
	public void destroySucceeded(IndexerCloudletState state,
			CallbackArguments<IndexerCloudletState> arguments) {
		state.metadataStore = null;
	}

	private void createFeedMetaData(IndexerCloudletState state, String key,
			Object extra) {
		JSONObject feedMetaData = new JSONObject();
		try {
			feedMetaData.put("key", key);
			feedMetaData.put("sequence", 0);
			state.metadataStore.set(key, feedMetaData, extra);
		} catch (JSONException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

	@Override
	public void setSucceeded(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		IndexWorkflow.onMetadataStored(arguments);
	}

	@Override
	public void setFailed(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		handleError(arguments);
	}

	@Override
	public void getSucceeded(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		String key = arguments.getKey();
		MosaicLogger.getLogger().trace(
				"succeeded fetch (" + MetadataKVCallback.BUCKET_NAME + ","
						+ key + ")");
		Object value = arguments.getValue();
		if (value == null) {
			createFeedMetaData(state, key, arguments.getExtra());
		} else {
			IndexWorkflow.findNewFeeds(arguments.getValue(),
					arguments.getExtra());
		}
	}

	@Override
	public void getFailed(IndexerCloudletState state,
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		handleError(arguments);
	}

	private void handleError(
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		String key = arguments.getKey();
		MosaicLogger.getLogger().warn(
				"failed fetch (" + MetadataKVCallback.BUCKET_NAME + "," + key
						+ ")");
		Map<String, String> errorMssg = new HashMap<String, String>(4);
		errorMssg.put("reason", "unexpected key-value store error");
		errorMssg.put("message", arguments.getValue().toString());
		errorMssg.put("bucket", MetadataKVCallback.BUCKET_NAME);
		errorMssg.put("key", key);
		IndexWorkflow.onIndexError(errorMssg, arguments.getExtra());
	}
}
