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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.examples.realtime_feeds.indexer.IndexerCloudlet.IndexerCloudletContext;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.syndication.io.FeedException;

public class IndexWorkflow {

    private static final String INDEX_TASK_TYPE = "index-data";
    private static Map<UUID, IndexWorkflow> indexers = new HashMap<UUID, IndexWorkflow>();
    private static MosaicLogger logger = MosaicLogger
            .createLogger(IndexWorkflow.class);

    private UUID key;

    private final JSONObject indexMessage;

    private final IndexerCloudletContext context;

    private Timeline currentTimeline;

    private final FeedParser parser;

    private JSONObject currentFeedMetaData;

    private final JSONObject newFeedTask;

    private JSONObject previousFeedMetaData;

    private final JSONArray newFeedItems;
    private boolean indexDone = false;
    private JSONObject newTimeline;

    private static final IndexWorkflow createIndexer(
            IndexerCloudletContext context, JSONObject recvMessage) {
        final IndexWorkflow aIndexer = new IndexWorkflow(context, recvMessage);
        aIndexer.key = UUID.randomUUID();
        IndexWorkflow.indexers.put(aIndexer.key, aIndexer);
        return aIndexer;
    }

    public static void findNewFeeds(JSONObject fetchedData, Object extra) {
        getIndexer((UUID) extra).doFeedDiff(fetchedData);
    }

    private static final IndexWorkflow getIndexer(UUID key) {
        return IndexWorkflow.indexers.get(key);
    }

    public static void indexNewFeed(IndexerCloudletContext context,
            JSONObject recvMessage) {
        final IndexWorkflow aIndexer = IndexWorkflow.createIndexer(context,
                recvMessage);
        IndexWorkflow.logger.trace("New indexer created for message "
                + aIndexer.indexMessage);
        aIndexer.fetchLatestFeed();
    }

    public static void onIndexError(Map<String, String> errorMessages) {
        final StringBuilder errorBuilder = new StringBuilder();
        for (final Map.Entry<String, String> entry : errorMessages.entrySet()) {
            errorBuilder.append(entry.getKey() + ": " + entry.getValue());
        }
        IndexWorkflow.logger.error(errorBuilder.toString());
    }

    public static void onMetadataStored(
            KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments) {
        getIndexer(arguments.getExtra()).handleMetadataStored(arguments);
    }

    /**
     * Parses latest fetched data.
     * 
     * @param fetchedData
     *            the fetched data
     */
    public static void parseLatestFeed(byte[] fetchedData, UUID extra) {
        getIndexer(extra).parseFeed(fetchedData);
    }

    public static void updateFeedMetadata(UUID extra) {
        getIndexer(extra).handleMetadataUpdate();
    }

    private IndexWorkflow(IndexerCloudletContext context, JSONObject recvMessage) {
        super();
        this.parser = new FeedParser();
        this.currentFeedMetaData = new JSONObject();
        this.newFeedTask = new JSONObject();
        this.newFeedItems = new JSONArray();
        this.context = context;
        this.indexMessage = recvMessage;
    }

    private void doFeedDiff(JSONObject fetchedData) {
        IndexWorkflow.logger.trace("indexing " + IndexWorkflow.INDEX_TASK_TYPE
                + " step 4 (diff-ing latest feed)...");
        String currentKey, currentURL, currentFeed, currentFeedId;
        long currentTimestamp;
        int currentSequence;
        try {
            this.previousFeedMetaData = fetchedData;
            final int previousSequence = this.previousFeedMetaData
                    .getInt("sequence");
            long minItemTimestamp = -1;
            try {
                minItemTimestamp = this.previousFeedMetaData
                        .getLong("timestamp");
            } catch (final JSONException e) {
            }
            long maxItemTimestamp = -1;
            maxItemTimestamp = minItemTimestamp;
            final List<Timeline.Entry> currentItems = new ArrayList<Timeline.Entry>();
            for (final Timeline.Entry item : this.currentTimeline.getEntries()) {
                if ((minItemTimestamp == -1)
                        || (item.getTimestamp() > minItemTimestamp)) {
                    currentItems.add(item);
                    if ((maxItemTimestamp == -1)
                            || (item.getTimestamp() > maxItemTimestamp)) {
                        maxItemTimestamp = item.getTimestamp();
                    }
                }
            }
            if (currentItems.size() > 0) {
                // NOTE: setup meta-data
                currentKey = this.previousFeedMetaData.getString("key");
                currentURL = this.indexMessage.getString("url");
                currentFeed = currentKey;
                currentFeedId = this.currentTimeline.getId();
                currentTimestamp = maxItemTimestamp;
                currentSequence = previousSequence + 1;
                this.currentFeedMetaData.put("key", currentKey);
                this.currentFeedMetaData.put("url", currentURL);
                this.currentFeedMetaData.put("feed", currentFeed);
                this.currentFeedMetaData.put("id", currentFeedId);
                this.currentFeedMetaData.put("timestamp", currentTimestamp);
                this.currentFeedMetaData.put("sequence", currentSequence);
                // NOTE: setup new timeline
                this.newTimeline = new JSONObject();
                final String newTimelineKey = StoreUtils
                        .generateFeedTimelineKey(currentURL, currentSequence);
                this.newTimeline.put("key", newTimelineKey);
                this.newTimeline.put("url", currentURL);
                this.newTimeline.put("feed", currentFeed);
                this.newTimeline.put("id", currentFeedId);
                this.newTimeline.put("timestamp", currentTimestamp);
                // NOTE: generate keys for new items
                final JSONArray items = new JSONArray();
                for (final Timeline.Entry item : currentItems) {
                    final String itemKey = StoreUtils.generateFeedItemKey(
                            currentURL, item.getId());
                    items.put(itemKey);
                    item.setKey(itemKey);
                    // NOTE: store item
                    final JSONObject json = item.convertToJson();
                    json.put("feed", currentFeed);
                    this.newFeedItems.put(json);
                    this.context.itemsStore.set(itemKey, json, this.key);
                }
                this.newTimeline.put("items", items);
                this.currentFeedMetaData.put("timelines",
                        this.newTimeline.getString("key"));
                IndexWorkflow.logger.trace("Current timeline has  "
                        + items.length() + " new items.");
                JSONArray prevTimelines = null;
                try {
                    prevTimelines = this.previousFeedMetaData
                            .getJSONArray("timelines");
                } catch (final JSONException e) {
                }
                final JSONArray newMetadataTimelines = new JSONArray();
                newMetadataTimelines.put(newTimelineKey);
                if (prevTimelines != null) {
                    for (int i = 0; (i < prevTimelines.length()) && (i < 9); i++) {
                        newMetadataTimelines.put(prevTimelines.getString(i));
                    }
                }
                // NOTE: store timeline
                this.context.timelinesStore.set(newTimelineKey,
                        this.newTimeline, null);
            } else {
                this.currentFeedMetaData = this.previousFeedMetaData;
                storeIndexOutcome();
            }
            this.indexDone = true;
        } catch (final JSONException e) {
            ExceptionTracer.traceDeferred(e);
        }
    }

    /**
     * Fetches latest feed data from the feeds-data bucket in the key-value
     * store.
     * 
     */
    private void fetchLatestFeed() {
        try {
            IndexWorkflow.logger.info("indexing "
                    + this.indexMessage.getString("url")
                    + " (from data) step 2 (fetching latest data)...");
            this.context.dataStore.get(this.indexMessage.getString("data"),
                    this.key);
        } catch (final JSONException e) {
            handleError(e);
        }
    }

    private void handleError(Exception e) {
        ExceptionTracer.traceDeferred(e);
        final Map<String, String> errorMssg = new HashMap<String, String>();
        errorMssg.put("reason", "unexpected parsing error");
        errorMssg.put("message", e.getMessage());
        onIndexError(errorMssg);
    }

    private void handleMetadataStored(
            KvStoreCallbackCompletionArguments<JSONObject, UUID> arguments) {
        if (this.indexDone) {
            storeIndexOutcome();
        } else {
            this.context.metadataStore.get(arguments.getKey(), this.key);
        }
    }

    private void handleMetadataUpdate() {
        IndexWorkflow.logger.trace("indexing " + IndexWorkflow.INDEX_TASK_TYPE
                + " step 5 (updating meta-data)...");
        try {
            this.context.metadataStore.set(
                    this.currentFeedMetaData.getString("key"),
                    this.currentFeedMetaData, this.key);
        } catch (final JSONException e) {
            ExceptionTracer.traceDeferred(e);
        }
    }

    private void indexFeed() throws JSONException {
        final String feedKey = StoreUtils.generateFeedKey(this.indexMessage
                .getString("url"));
        IndexWorkflow.logger.trace("indexing " + IndexWorkflow.INDEX_TASK_TYPE
                + " step 3 (fetching latest meta-data)...");
        // FIXME: ??? (I don't remember what the problem was...)
        this.context.metadataStore.get(feedKey, this.key);
    }

    /**
     * Parses latest fetched data.
     * 
     * @param fetchedData
     *            the fetched data
     */
    private void parseFeed(byte[] fetchedData) {
        try {
            IndexWorkflow.logger.trace("indexing "
                    + this.indexMessage.getString("url")
                    + " (from data) step 2 (parsing latest data)...");
            final byte[] data = fetchedData;
            this.currentTimeline = this.parser.parseFeed(data);
            indexFeed();
        } catch (final IOException e) {
            handleError(e);
        } catch (final FeedException e) {
            handleError(e);
        } catch (final JSONException e) {
            handleError(e);
        }
    }

    private void storeIndexOutcome() {
        IndexWorkflow.logger.trace("indexing " + IndexWorkflow.INDEX_TASK_TYPE
                + " step 6 (updating index task)...");
        try {
            final String feedTaskKey = StoreUtils.generateFeedTaskKey(
                    this.indexMessage.getString("url"),
                    IndexWorkflow.INDEX_TASK_TYPE);
            this.newFeedTask.put("key", feedTaskKey);
            this.newFeedTask.put("type", IndexWorkflow.INDEX_TASK_TYPE);
            this.newFeedTask.put("feed", this.currentFeedMetaData.get("key"));
            this.newFeedTask.put("url", this.indexMessage.getString("url"));
            this.newFeedTask.put("currentMetaData", this.currentFeedMetaData);
            this.newFeedTask.put("previousMetaData", this.previousFeedMetaData);
            this.newFeedTask.put("timeline", this.newTimeline);
            final JSONArray items = new JSONArray();
            for (int i = 0; i < this.newFeedItems.length(); i++) {
                final JSONObject item = this.newFeedItems.getJSONObject(i);
                items.put(item.getString("key"));
            }
            this.newFeedTask.put("items", items);
            final Object error = null;
            this.newFeedTask.put("error", error);
            this.context.tasksStore.set(feedTaskKey, this.newFeedTask, null);
        } catch (final JSONException e) {
            ExceptionTracer.traceDeferred(e);
        }
    }
}
