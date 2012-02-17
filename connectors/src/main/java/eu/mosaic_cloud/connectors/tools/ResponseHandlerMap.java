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


import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCanceled;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;


/**
 * Implements a Map between request (response) identifier and response handlers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseHandlerMap
		extends Object
{
	public ResponseHandlerMap ()
	{
		super ();
		this.futures = new ConcurrentHashMap<String, CallbackCompletionDeferredFuture<?>> ();
	}
	
	/**
	 * Removes from the map the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public void cancel (final String request)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		if (future == null)
			return;
		Preconditions.checkState (future != null);
		future.trigger.triggerFailed (new CallbackCanceled ());
	}
	
	/**
	 * Removes from the handler map all pending requests.
	 */
	public void cancelAll ()
	{
		synchronized (this.futures) {
			for (final CallbackCompletionDeferredFuture<?> future : this.futures.values ())
				future.trigger.triggerFailed (new CallbackCanceled ());
			this.futures.clear ();
		}
	}
	
	/**
	 * Removes from the map the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public void fail (final String request, final Throwable exception)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		if (future == null)
			return;
		Preconditions.checkState (future != null);
		future.trigger.triggerFailed (new CallbackCanceled ());
	}
	
	public CallbackCompletionDeferredFuture<?> peek (final String request)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		if (future == null)
			return (null);
		Preconditions.checkState (future != null);
		return (future);
	}
	
	/**
	 * Add handlers for the response of the request with the given identifier.
	 * If other handlers have been added previously for the request, these
	 * handlers will be appended to the existing ones.
	 * 
	 * @param request
	 *            the request identifier
	 * @param handlers
	 *            the list of handlers to add
	 */
	public <O extends Object> void register (final String request, final CallbackCompletionDeferredFuture<O> future)
	{
		Preconditions.checkNotNull (request);
		Preconditions.checkNotNull (future);
		this.futures.put (request, future);
	}
	
	/**
	 * Returns the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public void succeed (final String request, final Object outcome)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		if (future == null)
			return;
		Preconditions.checkState (future != null);
		((CallbackCompletionDeferredFuture<Object>) future).trigger.triggerSucceeded (outcome);
	}
	
	protected final ConcurrentHashMap<String, CallbackCompletionDeferredFuture<?>> futures;
}
