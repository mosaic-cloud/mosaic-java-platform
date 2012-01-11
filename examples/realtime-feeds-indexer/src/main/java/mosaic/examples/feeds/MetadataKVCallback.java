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

import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.kvstore.DefaultKeyValueAccessorCallback;
import eu.mosaic_cloud.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
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
