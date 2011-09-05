package mosaic.examples.feeds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mosaic.cloudlet.resources.amqp.AmqpQueueConsumeMessage;
import mosaic.cloudlet.resources.kvstore.KeyValueCallbackArguments;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.examples.feeds.IndexerCloudlet.IndexerCloudletState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.syndication.io.FeedException;

public class IndexWorkflow {

	private static final String INDEX_TASK_TYPE = "index-data";
	private static Map<UUID, IndexWorkflow> indexers = new HashMap<UUID, IndexWorkflow>();;

	private AmqpQueueConsumeMessage<JSONObject> recvMessage = null;
	private UUID key;
	private JSONObject indexMessage;
	private IndexerCloudletState state;
	private Timeline currentTimeline;
	private FeedParser parser;
	private JSONObject currentFeedMetaData;
	private JSONObject newFeedTask;
	private JSONObject previousFeedMetaData;
	private JSONArray newFeedItems;
	private boolean indexDone = false;
	private JSONObject newTimeline;

	private IndexWorkflow(IndexerCloudletState state,
			AmqpQueueConsumeMessage<JSONObject> recvMessage) {
		super();
		this.parser = new FeedParser();
		this.currentFeedMetaData = new JSONObject();
		this.newFeedTask = new JSONObject();
		this.newFeedItems = new JSONArray();
		this.state = state;
		this.recvMessage = recvMessage;
		this.indexMessage = recvMessage.getData();
	}

	private static final IndexWorkflow createIndexer(
			IndexerCloudletState state,
			AmqpQueueConsumeMessage<JSONObject> recvMessage) {
		IndexWorkflow aIndexer = new IndexWorkflow(state, recvMessage);
		aIndexer.key = UUID.randomUUID();
		IndexWorkflow.indexers.put(aIndexer.key, aIndexer);
		return aIndexer;
	}

	private static final IndexWorkflow getIndexer(UUID key) {
		return IndexWorkflow.indexers.get(key);
	}

	public static void indexNewFeed(IndexerCloudletState state,
			AmqpQueueConsumeMessage<JSONObject> recvMessage) {

		IndexWorkflow aIndexer = IndexWorkflow
				.createIndexer(state, recvMessage);
		MosaicLogger.getLogger().trace(
				"New indexer created for message " + aIndexer.indexMessage);
		aIndexer.fetchLatestFeed();
	}

	/**
	 * Fetches latest feed data from the feeds-data bucket in the key-value
	 * store.
	 * 
	 */
	private void fetchLatestFeed() {
		try {
			MosaicLogger.getLogger().info(
					"indexing " + this.indexMessage.getString("url")
							+ " (from data) step 2 (fetching latest data)...");
			this.state.dataStore.get(this.indexMessage.getString("data"),
					this.key);
		} catch (JSONException e) {
			handleError(e);
		}

	}

	/**
	 * Parses latest fetched data.
	 * 
	 * @param fetchedData
	 *            the fetched data
	 */
	public static void parseLatestFeed(Object fetchedData, Object extra) {
		getIndexer((UUID) extra).parseFeed(fetchedData);
	}

	/**
	 * Parses latest fetched data.
	 * 
	 * @param fetchedData
	 *            the fetched data
	 */
	private void parseFeed(Object fetchedData) {
		try {
			MosaicLogger.getLogger().trace(
					"indexing " + this.indexMessage.getString("url")
							+ " (from data) step 2 (parsing latest data)...");
			byte[] data = (byte[]) fetchedData;
			this.currentTimeline = this.parser.parseFeed(data);
			indexFeed();
		} catch (IOException e) {
			handleError(e);
		} catch (FeedException e) {
			handleError(e);
		} catch (JSONException e) {
			handleError(e);
		}
	}

	private void indexFeed() throws JSONException {
		String feedKey = StoreUtils.generateFeedKey(this.indexMessage
				.getString("url"));
		MosaicLogger.getLogger().trace(
				"indexing " + IndexWorkflow.INDEX_TASK_TYPE
						+ " step 3 (fetching latest meta-data)...");
		// FIXME
		this.state.metadataStore.get(feedKey, this.key);
	}

	public static void findNewFeeds(Object fetchedData, Object extra) {
		getIndexer((UUID) extra).doFeedDiff(fetchedData);
	}

	private void doFeedDiff(Object fetchedData) {
		MosaicLogger.getLogger().trace(
				"indexing " + IndexWorkflow.INDEX_TASK_TYPE
						+ " step 4 (diff-ing latest feed)...");

		String currentKey, currentURL, currentFeed, currentFeedId;
		long currentTimestamp;
		int currentSequence;

		try {
			this.previousFeedMetaData = (JSONObject) fetchedData;
			int previousSequence = this.previousFeedMetaData.getInt("sequence");
			long minItemTimestamp = -1;
			try {
				minItemTimestamp = this.previousFeedMetaData
						.getLong("timestamp");
			} catch (JSONException e) {
			}
			long maxItemTimestamp = -1;
			maxItemTimestamp = minItemTimestamp;

			List<Timeline.Entry> currentItems = new ArrayList<Timeline.Entry>();
			for (Timeline.Entry item : this.currentTimeline.getEntries()) {
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
				// setup meta-data
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

				// setup new timeline
				this.newTimeline = new JSONObject();
				String newTimelineKey = StoreUtils.generateFeedTimelineKey(
						currentURL, currentSequence);
				this.newTimeline.put("key", newTimelineKey);
				this.newTimeline.put("url", currentURL);
				this.newTimeline.put("feed", currentFeed);
				this.newTimeline.put("id", currentFeedId);
				this.newTimeline.put("timestamp", currentTimestamp);

				// generate keys for new items
				JSONArray items = new JSONArray();
				for (Timeline.Entry item : currentItems) {
					String itemKey = StoreUtils.generateFeedItemKey(currentURL,
							item.getId());
					items.put(itemKey);
					item.setKey(itemKey);

					// store item
					JSONObject json = item.convertToJson();
					json.put("feed", currentFeed);
					this.newFeedItems.put(json);
					this.state.itemsStore.set(itemKey, json, this.key);
				}
				this.newTimeline.put("items", items);
				this.currentFeedMetaData.put("timelines",
						this.newTimeline.getString("key"));

				MosaicLogger.getLogger().trace(
						"Current timeline has  " + items.length()
								+ " new items.");
				JSONArray prevTimelines = null;
				try {
					prevTimelines = this.previousFeedMetaData
							.getJSONArray("timelines");
				} catch (JSONException e) {
				}

				JSONArray newMetadataTimelines = new JSONArray();
				newMetadataTimelines.put(newTimelineKey);
				if (prevTimelines != null) {
					for (int i = 0; (i < prevTimelines.length()) && (i < 9); i++) {
						newMetadataTimelines.put(prevTimelines.getString(i));
					}
				}

				// store timeline
				this.state.timelinesStore.set(newTimelineKey, this.newTimeline,
						this.key);
			} else {
				this.currentFeedMetaData = this.previousFeedMetaData;
				storeIndexOutcome();
			}

			this.indexDone = true;
		} catch (JSONException e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	public static void updateFeedMetadata(Object extra) {
		getIndexer((UUID) extra).handleMetadataUpdate();
	}

	private void handleMetadataUpdate() {
		MosaicLogger.getLogger().trace(
				"indexing " + IndexWorkflow.INDEX_TASK_TYPE
						+ " step 5 (updating meta-data)...");
		try {
			this.state.metadataStore.set(
					this.currentFeedMetaData.getString("key"),
					this.currentFeedMetaData, this.key);
		} catch (JSONException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

	public static void onMetadataStored(
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		getIndexer((UUID) arguments.getExtra()).handleMetadataStored(arguments);
	}

	private void handleMetadataStored(
			KeyValueCallbackArguments<IndexerCloudletState> arguments) {
		if (this.indexDone) {
			storeIndexOutcome();
		} else {
			this.state.metadataStore.get(arguments.getKey(), this.key);
		}
	}

	private void storeIndexOutcome() {
		MosaicLogger.getLogger().trace(
				"indexing " + IndexWorkflow.INDEX_TASK_TYPE
						+ " step 6 (updating index task)...");

		try {
			String feedTaskKey = StoreUtils.generateFeedTaskKey(
					this.indexMessage.getString("url"),
					IndexWorkflow.INDEX_TASK_TYPE);
			this.newFeedTask.put("key", feedTaskKey);
			this.newFeedTask.put("type", IndexWorkflow.INDEX_TASK_TYPE);
			this.newFeedTask.put("feed", this.currentFeedMetaData.get("key"));
			this.newFeedTask.put("url", this.indexMessage.getString("url"));
			this.newFeedTask.put("currentMetaData", this.currentFeedMetaData);
			this.newFeedTask.put("previousMetaData", this.previousFeedMetaData);
			this.newFeedTask.put("timeline", this.newTimeline);

			JSONArray items = new JSONArray();
			for (int i = 0; i < this.newFeedItems.length(); i++) {
				JSONObject item = this.newFeedItems.getJSONObject(i);
				items.put(item.getString("key"));
			}
			this.newFeedTask.put("items", items);
			Object error = null;
			this.newFeedTask.put("error", error);
			this.state.taskStore.set(feedTaskKey, this.newFeedTask, this.key);
		} catch (JSONException e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	public static void sendAcknowledge(Object extra) {
		getIndexer((UUID) extra).recvMessage.acknowledge();
		MosaicLogger.getLogger().trace("finished indexing...");
	}

	private void handleError(Exception e) {
		ExceptionTracer.traceDeferred(e);
		Map<String, String> errorMssg = new HashMap<String, String>();
		errorMssg.put("reason", "unexpected parsing error");
		errorMssg.put("message", e.getMessage());
		onIndexError(errorMssg, this.key);
	}

	public static void onIndexError(Map<String, String> errorMessages,
			Object extra) {
		StringBuilder errorBuilder = new StringBuilder();
		for (Map.Entry<String, String> entry : errorMessages.entrySet()) {
			errorBuilder.append(entry.getKey() + ": " + entry.getValue());
		}
		MosaicLogger.getLogger().error(errorBuilder.toString());
		// getIndexer((UUID) extra).recvMessage.acknowledge();
	}

}
