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

package eu.mosaic_cloud.connectors.kvstore.memcache;

import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Connector for key-value distributed storage systems implementing the
 * memcached protocol.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 */
public class MemcacheKvStoreConnector<TValue extends Object> extends
        BaseKvStoreConnector<TValue, MemcacheKvStoreConnectorProxy<TValue>> implements
        IMemcacheKvStoreConnector<TValue> {

    protected MemcacheKvStoreConnector(final MemcacheKvStoreConnectorProxy<TValue> proxy) {
        super(proxy);
    }

    /**
     * Creates the connector.
     * 
     * @param configuration
     *            the execution environment of a connector
     * @param encoder
     *            encoder used for serializing and deserializing data stored in
     *            the key-value store
     * @return the connector
     * @throws Throwable
     */
    public static <T extends Object> MemcacheKvStoreConnector<T> create(
            final ConnectorConfiguration configuration, final DataEncoder<T> encoder) {
        final MemcacheKvStoreConnectorProxy<T> proxy = MemcacheKvStoreConnectorProxy.create(
                configuration, encoder);
        return new MemcacheKvStoreConnector<T>(proxy);
    }

    @Override
    public CallbackCompletion<Boolean> add(final String key, final int exp, final TValue data) {
        return this.proxy.add(key, exp, data);
    }

    @Override
    public CallbackCompletion<Boolean> append(final String key, final TValue data) {
        return this.proxy.append(key, data);
    }

    @Override
    public CallbackCompletion<Boolean> cas(final String key, final TValue data) {
        return this.proxy.cas(key, data);
    }

    @Override
    public CallbackCompletion<Map<String, TValue>> getBulk(final List<String> keys) {
        return this.proxy.getBulk(keys);
    }

    @Override
    public CallbackCompletion<Boolean> prepend(final String key, final TValue data) {
        return this.proxy.prepend(key, data);
    }

    @Override
    public CallbackCompletion<Boolean> replace(final String key, final int exp, final TValue data) {
        return this.proxy.replace(key, exp, data);
    }

    @Override
    public CallbackCompletion<Boolean> set(final String key, final int exp, final TValue data) {
        return this.proxy.set(key, exp, data);
    }
}
