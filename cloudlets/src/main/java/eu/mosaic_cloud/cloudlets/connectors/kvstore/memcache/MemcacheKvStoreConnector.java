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

package eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache;

import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

/**
 * Cloudlet-level connector for memcached-based key value storages. Cloudlets
 * will use an object of this type to get access to a memcached storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            connector callback context type
 * @param <D>
 *            type of data stored in the key-value store
 * @param <E>
 *            type of extra data used for correlation of messages exchanged with
 *            the key-value store (e.g. get - getSucceded)
 */
public class MemcacheKvStoreConnector<C, D, E>
        extends
        BaseKvStoreConnector<eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<D>, IMemcacheKvStoreConnectorCallback<C, D, E>, C, D, E> {

    /**
     * Creates a new accessor.
     * 
     * @param config
     *            configuration data required by the accessor
     * @param cloudlet
     *            the cloudlet controller of the cloudlet using the accessor
     */
    public MemcacheKvStoreConnector(
            final ICloudletController<?> cloudlet,
            final eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<D> connector,
            final IConfiguration config, final IMemcacheKvStoreConnectorCallback<C, D, E> callback,
            final C context) {
        super(cloudlet, connector, config, callback, context);
    }

    public CallbackCompletion<Boolean> set(final String key, final D value, int exp, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.set(key, exp, value);
        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {
                @Override
                public CallbackCompletion<Void> completed(final CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() != null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.setSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.setFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Boolean> add(final String key, final D value, int exp, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.add(key, exp, value);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.addSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.addFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Boolean> append(final String key, final D value, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.append(key, value);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.appendSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.appendFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Boolean> prepend(final String key, final D value, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.prepend(key, value);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.prependSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.prependFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Boolean> cas(final String key, final D value, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.cas(key, value);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.casSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.casFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Boolean> replace(final String key, final D value, int exp, final E extra) {
        final CallbackCompletion<Boolean> completion = this.connector.replace(key,exp,value);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.replaceSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.replaceFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, D, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, key, (D) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

    public CallbackCompletion<Map<String, D>> getBulk(final List<String> keys, final E extra) {
        final CallbackCompletion<Map<String, D>> completion = this.connector.getBulk(keys);

        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @Override
                public CallbackCompletion<Void> completed(CallbackCompletion<?> aCompletion) {
                    assert (aCompletion == completion);
                    CallbackCompletion<Void> resultCompletion;
                    if (completion.getException() == null) {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.getBulkSucceeded(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, Map<String,D>, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, keys, completion.getOutcome(), extra));
                    } else {
                        resultCompletion = MemcacheKvStoreConnector.this.callback.getBulkFailed(
                                MemcacheKvStoreConnector.this.context,
                                new KvStoreCallbackCompletionArguments<C, Map<String,D>, E>(
                                        MemcacheKvStoreConnector.this.cloudlet, keys, (Map<String,D>) completion
                                                .getException(), extra));
                    }
                    return resultCompletion;
                }
            });
        }
        return completion;
    }

}
