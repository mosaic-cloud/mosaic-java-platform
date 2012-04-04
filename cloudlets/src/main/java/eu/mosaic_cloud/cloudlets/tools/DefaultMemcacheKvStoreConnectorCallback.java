/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.tools;

import java.util.Map;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.IMemcacheKvStoreConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Default memcached key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TContext>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultMemcacheKvStoreConnectorCallback<TContext, TData, TExtra> extends
        DefaultKvStoreConnectorCallback<TContext, TData, TExtra> implements
        IMemcacheKvStoreConnectorCallback<TContext, TData, TExtra> {

    @Override
    public CallbackCompletion<Void> addFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Add Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> addSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Add Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> appendFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Append Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> appendSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Append Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> casFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Cas Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> casSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Cas Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> getBulkFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<Map<String, TData>, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "GetBulk Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> getBulkSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<Map<String, TData>, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "GetBulk Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> prependFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Prepend Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> prependSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Prepend Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> replaceFailed(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Replace Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> replaceSucceeded(final TContext context,
            final KvStoreCallbackCompletionArguments<TData, TExtra> arguments) {
        return this.handleUnhandledCallback(arguments, "Replace Succeeded", true, false);
    }
}
