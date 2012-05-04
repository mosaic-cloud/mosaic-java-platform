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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultKvStoreConnectorCallback;
import eu.mosaic_cloud.examples.realtime_feeds.indexer.IndexerCloudlet.IndexerCloudletContext;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import org.json.JSONException;
import org.json.JSONObject;


public final class MetadataKVCallback
		extends DefaultKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, UUID>
{
	@Override
	public CallbackCompletion<Void> destroySucceeded (final IndexerCloudletContext context, final CallbackArguments arguments)
	{
		context.metadataStore = null;
		return ICallback.SUCCESS;
	}
	
	@Override
	public CallbackCompletion<Void> getFailed (final IndexerCloudletContext context, final KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments)
	{
		this.handleError (arguments);
		return ICallback.SUCCESS;
	}
	
	@Override
	public CallbackCompletion<Void> getSucceeded (final IndexerCloudletContext context, final KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments)
	{
		final String key = arguments.getKey ();
		this.logger.debug ("succeeded fetch (" + MetadataKVCallback.BUCKET_NAME + "," + key + ")");
		final JSONObject value = arguments.getValue ();
		if (value == null) {
			this.createFeedMetaData (context, key, arguments.getExtra ());
		} else {
			IndexWorkflow.findNewFeeds (arguments.getValue (), arguments.getExtra ());
		}
		return ICallback.SUCCESS;
	}
	
	@Override
	public CallbackCompletion<Void> setFailed (final IndexerCloudletContext context, final KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments)
	{
		this.handleError (arguments);
		return ICallback.SUCCESS;
	}
	
	@Override
	public CallbackCompletion<Void> setSucceeded (final IndexerCloudletContext context, final KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments)
	{
		IndexWorkflow.onMetadataStored (arguments);
		return ICallback.SUCCESS;
	}
	
	private void createFeedMetaData (final IndexerCloudletContext context, final String key, final UUID extra)
	{
		final JSONObject feedMetaData = new JSONObject ();
		try {
			feedMetaData.put ("key", key);
			feedMetaData.put ("sequence", 0);
			context.metadataStore.set (key, feedMetaData, extra);
		} catch (final JSONException e) {
			ExceptionTracer.traceDeferred (e);
		}
	}
	
	private void handleError (final KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments)
	{
		final String key = arguments.getKey ();
		this.logger.warn ("failed fetch (" + MetadataKVCallback.BUCKET_NAME + "," + key + ")");
		final Map<String, String> errorMssg = new HashMap<String, String> (4);
		errorMssg.put ("reason", "unexpected key-value store error");
		errorMssg.put ("message", arguments.getValue ().toString ());
		errorMssg.put ("bucket", MetadataKVCallback.BUCKET_NAME);
		errorMssg.put ("key", key);
		IndexWorkflow.onIndexError (errorMssg);
	}
	
	private static final String BUCKET_NAME = "feed-metadata";
}
