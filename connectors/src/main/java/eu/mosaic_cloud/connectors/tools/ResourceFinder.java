/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.tools;


import java.util.Map;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.connectors.tools.ResourceComponentCallbacks.ResourceType;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.idl.ChannelData;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;


/**
 * Finder for resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResourceFinder
{
	private ResourceFinder ()
	{}
	
	/**
	 * Starts an asynchronous driver lookup. When the result from the mOSAIC
	 * platform arrives the provided callback will be invoked.
	 * 
	 * @param type
	 *            the type of resource to find
	 * @param threading
	 *            threading context for creating threads
	 * @param callback
	 *            the callback to be called when the resource is found
	 */
	public void findResource (final ResourceType type, final ThreadingContext threading, final IFinderCallback callback)
	{
		ResourceFinder.logger.trace ("ResourceFinder - find resource");
		final DeferredFuture<ComponentCallReply> replyFuture = ResourceComponentCallbacks.callbacks.findDriver (type);
		final Worker worker = new Worker (replyFuture, callback);
		Threading.createAndStartDaemonThread (threading, this, "callback", worker);
	}
	
	/**
	 * Returns a finder object.
	 * 
	 * @return the finder object
	 */
	public static ResourceFinder getResourceFinder ()
	{
		if (ResourceFinder.finder == null) {
			ResourceFinder.finder = new ResourceFinder ();
		}
		return ResourceFinder.finder;
	}
	
	private static ResourceFinder finder;
	private static MosaicLogger logger = MosaicLogger.createLogger (ResourceFinder.class);
	
	class Worker
			implements
				Runnable
	{
		public Worker (final DeferredFuture<ComponentCallReply> future, final IFinderCallback callback)
		{
			this.future = future;
			this.callback = callback;
		}
		
		@Override
		public void run ()
		{
			ComponentCallReply reply;
			ChannelData channel = null;
			try {
				reply = this.future.get ();
				if (reply.outputsOrError instanceof Map) {
					@SuppressWarnings ("unchecked") final Map<String, String> outcome = (Map<String, String>) reply.outputsOrError;
					channel = new ChannelData (outcome.get ("channelIdentifier"), outcome.get ("channelEndpoint"));
					ResourceFinder.logger.debug ("Found driver on channel " + channel);
					this.callback.resourceFound (channel);
				} else {
					this.callback.resourceNotFound ();
				}
			} catch (final InterruptedException e) {
				ExceptionTracer.traceIgnored (e);
				this.callback.resourceNotFound ();
			} catch (final ExecutionException e) {
				ExceptionTracer.traceIgnored (e);
				this.callback.resourceNotFound ();
			} catch (final Throwable e) {
				ExceptionTracer.traceIgnored (e);
				this.callback.resourceNotFound ();
			}
		}
		
		private final IFinderCallback callback;
		private final DeferredFuture<ComponentCallReply> future;
	}
}
