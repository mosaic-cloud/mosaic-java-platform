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

import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultKvStoreConnectorCallback;
import eu.mosaic_cloud.examples.realtime_feeds.indexer.IndexerCloudlet.IndexerCloudletContext;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import org.json.JSONObject;

public class ItemsKVCallback
        extends
        DefaultKvStoreConnectorCallback<IndexerCloudletContext, JSONObject, Void> {

    private static final String BUCKET_NAME = "feed-items";

    @Override
    public CallbackCompletion<Void> destroySucceeded(
            IndexerCloudletContext context,
            CallbackArguments<IndexerCloudletContext> arguments) {
        context.itemsStore = null;
        return ICallback.SUCCESS;
    }

    private void handleError(
            KvStoreCallbackCompletionArguments<IndexerCloudletContext, JSONObject, Void> arguments) {
        final String key = arguments.getKey();
        this.logger.warn("failed fetch (" + ItemsKVCallback.BUCKET_NAME + ","
                + key + ")");
        final Map<String, String> errorMssg = new HashMap<String, String>(4);
        errorMssg.put("reason", "unexpected key-value store error");
        errorMssg.put("message", arguments.getValue().toString());
        errorMssg.put("bucket", ItemsKVCallback.BUCKET_NAME);
        errorMssg.put("key", key);
        IndexWorkflow.onIndexError(errorMssg);
    }

    @Override
    public CallbackCompletion<Void> setFailed(
            IndexerCloudletContext context,
            KvStoreCallbackCompletionArguments<IndexerCloudletContext, JSONObject, Void> arguments) {
        handleError(arguments);
        return ICallback.SUCCESS;
    }
}
