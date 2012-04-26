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

package eu.mosaic_cloud.connectors.core;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCanceled;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;


/**
 * Implements a Map between request (response) identifier and response handler
 * (callback).
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ResponseHandlerMap
{
	public ResponseHandlerMap (final Transcript transcript)
	{
		super ();
		this.futures = new ConcurrentHashMap<String, CallbackCompletionDeferredFuture<?>> ();
		this.transcript = transcript;
	}
	
	/**
	 * Cancels a request and removes from the map the handlers for a request and
	 * the actual request.
	 * 
	 * @param request
	 *            the request identifier
	 */
	public void cancel (final String request)
	{
		Preconditions.checkNotNull (request);
		this.transcript.traceDebugging ("calceling the pending request `%s`...", request);
		final CallbackCompletionDeferredFuture<?> future = this.removeRequest (request);
		Preconditions.checkState (future != null);
		future.trigger.triggerFailed (new CallbackCanceled ());
	}
	
	/**
	 * Cancels all pending requests and removes from the handler map all pending
	 * requests.
	 */
	public void cancelAll ()
	{
		synchronized (this.futures) {
			this.transcript.traceDebugging ("canceling all pending requests...");
			for (final Map.Entry<String, CallbackCompletionDeferredFuture<?>> entry : this.futures.entrySet ()) {
				final String request = entry.getKey ();
				final CallbackCompletionDeferredFuture<?> future = entry.getValue ();
				this.transcript.traceDebugging ("canceling the pending request `%s`...", request);
				future.trigger.triggerFailed (new CallbackCanceled ()); // NOPMD
			}
			this.futures.clear ();
		}
	}
	
	/**
	 * Removes from the map the handlers for a failed request and the actual
	 * request.
	 * 
	 * @param request
	 *            the request identifier
	 * @param exception
	 *            the cause of the request failure
	 */
	public void fail (final String request, final Throwable exception)
	{
		Preconditions.checkNotNull (request);
		Preconditions.checkNotNull (exception);
		this.transcript.traceDebugging ("failing the pending request `%s` with exception `%s` (`%s`)...", request, exception.getClass ().getName (), exception.getMessage ());
		final CallbackCompletionDeferredFuture<?> future = this.removeRequest (request);
		Preconditions.checkState (future != null);
		future.trigger.triggerFailed (exception);
	}
	
	/**
	 * Checks if the map contains an entry for the specified request. (It throws if no request exists.)
	 * 
	 * @param request
	 *            the request identifier
	 * @return the callback future for the request
	 */
	public CallbackCompletionDeferredFuture<?> peek (final String request)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.get (request);
		Preconditions.checkState (future != null);
		return future;
	}
	
	/**
	 * Checks if the map contains an entry for the specified request. (It returns null if no request exists.)
	 * 
	 * @param request
	 *            the request identifier
	 * @return the callback future for the request
	 */
	public CallbackCompletionDeferredFuture<?> peekMaybe (final String request)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.get (request);
		return future;
	}
	
	/**
	 * Add handler for the response of the request with the given identifier. If
	 * another handler has been added previously for the request, these handler
	 * will be replaced.
	 * 
	 * @param request
	 *            the request identifier
	 * @param future
	 *            the handler to set
	 */
	public void register (final String request, final CallbackCompletionDeferredFuture<?> future)
	{
		Preconditions.checkNotNull (request);
		Preconditions.checkNotNull (future);
		this.transcript.traceDebugging ("registering the pending request `%s`...", request);
		this.futures.put (request, future);
	}
	
	/**
	 * Removes a request entry from the map and triggers the callback for the
	 * response.
	 * 
	 * @param request
	 *            the request identifier
	 * @param outcome
	 *            the actual result for the request
	 */
	@SuppressWarnings ("unchecked")
	public void succeed (final String request, final Object outcome)
	{
		Preconditions.checkNotNull (request);
		this.transcript.traceDebugging ("succeeding the pending request `%s`...", request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		Preconditions.checkState (future != null);
		((CallbackCompletionDeferredFuture<Object>) future).trigger.triggerSucceeded (outcome);
	}
	
	/**
	 * Removes from the map the handlers for a request and the actual request.
	 * 
	 * @param request
	 *            the request identifier
	 */
	private CallbackCompletionDeferredFuture<?> removeRequest (final String request)
	{
		Preconditions.checkNotNull (request);
		final CallbackCompletionDeferredFuture<?> future = this.futures.remove (request);
		Preconditions.checkState (future != null);
		return future;
	}
	
	private final ConcurrentHashMap<String, CallbackCompletionDeferredFuture<? extends Object>> futures;
	private final Transcript transcript;
}
