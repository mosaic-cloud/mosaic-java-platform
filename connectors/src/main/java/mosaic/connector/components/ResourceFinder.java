package mosaic.connector.components;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import mosaic.connector.components.ResourceComponentCallbacks.ResourceType;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.interop.idl.ChannelData;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.tools.OutcomeFuture;

/**
 * Finder for resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResourceFinder {
	private static ResourceFinder finder;

	private ResourceFinder() {

	}

	/**
	 * Returns a finder object.
	 * 
	 * @return the finder object
	 */
	public static ResourceFinder getResourceFinder() {
		if (ResourceFinder.finder == null) {
			ResourceFinder.finder = new ResourceFinder();
		}
		return ResourceFinder.finder;
	}

	/**
	 * Starts an asynchronous driver lookup. When the result from the mOSAIC
	 * platform arrives the provided callback will be invoked.
	 * 
	 * @param type
	 *            the type of resource to find
	 * @param callback
	 *            the callback to be called when the resource is found
	 */
	public void findResource(ResourceType type, IFinderCallback callback) {
		MosaicLogger.getLogger().trace("ResourceFinder - find resource");
		OutcomeFuture<ComponentCallReply> replyFuture = ResourceComponentCallbacks.callbacks
				.findDriver(type);
		Worker worker = new Worker(replyFuture, callback);
		Thread tWorker = new Thread(worker);
		tWorker.start();
	}

	class Worker implements Runnable {
		private OutcomeFuture<ComponentCallReply> future;
		private IFinderCallback callback;

		public Worker(OutcomeFuture<ComponentCallReply> future,
				IFinderCallback callback) {
			this.future = future;
			this.callback = callback;
		}

		@Override
		public void run() {
			ComponentCallReply reply;
			ChannelData channel = null;
			try {
				reply = this.future.get();
				if (reply.outputsOrError instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> outcome = (Map<String, String>) reply.outputsOrError;
					channel = new ChannelData(outcome.get("channelIdentifier"),
							outcome.get("channelEndpoint"));
					MosaicLogger.getLogger().debug(
							"Found driver on channel " + channel);
					this.callback.resourceFound(channel);
				} else {
					this.callback.resourceNotFound();
				}
			} catch (InterruptedException e) {
				ExceptionTracer.traceDeferred(e);
				this.callback.resourceNotFound();
			} catch (ExecutionException e) {
				ExceptionTracer.traceDeferred(e);
				this.callback.resourceNotFound();
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
			}
		}

	}

}
