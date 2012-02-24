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

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCanceled;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;

/**
 * Implements a Map between request (response) identifier and response handler
 * (callback).
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseHandlerMap {

    private final ConcurrentHashMap<String, CallbackCompletionDeferredFuture<? extends Object>> futures;

    public ResponseHandlerMap() {
        super();
        this.futures = new ConcurrentHashMap<String, CallbackCompletionDeferredFuture<?>>();
    }

    /**
     * Cancels a request and removes from the map the handlers for a request and
     * the actual request.
     * 
     * @param request
     *            the request identifier
     */
    public void cancel(final String request) {
        final CallbackCompletionDeferredFuture<?> future = removeRequest(request);
        future.trigger.triggerFailed(new CallbackCanceled());
    }

    /**
     * Cancels all pending requests and removes from the handler map all pending
     * requests.
     */
    public void cancelAll() {
        synchronized (this.futures) {
            for (final CallbackCompletionDeferredFuture<?> future : this.futures.values()) {
                future.trigger.triggerFailed(new CallbackCanceled()); // NOPMD
                                                                      // by
                                                                      // georgiana
                                                                      // on
                                                                      // 2/20/12
                                                                      // 4:21 PM
            }
            this.futures.clear();
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
    public void fail(final String request, final Exception exception) {
        final CallbackCompletionDeferredFuture<?> future = removeRequest(request);
        future.trigger.triggerFailed(exception);
    }

    /**
     * Removes from the map the handlers for a request and the actual request.
     * 
     * @param request
     *            the request identifier
     */
    private CallbackCompletionDeferredFuture<?> removeRequest(final String request) {
        Preconditions.checkNotNull(request);
        final CallbackCompletionDeferredFuture<?> future = this.futures.remove(request);
        Preconditions.checkState(future != null);
        return future;

    }

    /**
     * Checks if the map contains an entry for the specified request.
     * 
     * @param request
     *            the request identifier
     * @return the callback future for the request
     */
    public CallbackCompletionDeferredFuture<?> peek(final String request) {
        Preconditions.checkNotNull(request);
        final CallbackCompletionDeferredFuture<?> future = this.futures.get(request);
        Preconditions.checkState(future != null);
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
    public <O extends Object> void register(final String request,
            final CallbackCompletionDeferredFuture<O> future) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(future);
        this.futures.put(request, future);
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
    @SuppressWarnings("unchecked")
    public void succeed(final String request, final Object outcome) {
        Preconditions.checkNotNull(request);
        final CallbackCompletionDeferredFuture<?> future = this.futures.remove(request);
        Preconditions.checkState(future != null);
        ((CallbackCompletionDeferredFuture<Object>) future).trigger.triggerSucceeded(outcome);
    }

}
