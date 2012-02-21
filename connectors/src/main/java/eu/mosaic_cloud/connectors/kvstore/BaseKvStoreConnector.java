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

package eu.mosaic_cloud.connectors.kvstore;

import java.util.List;

import eu.mosaic_cloud.connectors.core.BaseConnector;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Basic key-value store connector. This connector implements only the
 * operations common to most of the key-value store systems.
 * 
 * @author Georgiana Macariu
 * 
 * @param <D>
 *            type of stored data
 * @param <P>
 *            type of connector proxy
 */
public abstract class BaseKvStoreConnector<D extends Object, P extends BaseKvStoreConnectorProxy<D>>
        extends BaseConnector<P> implements IKvStoreConnector<D> {
    
    protected BaseKvStoreConnector(final P proxy) {
        super(proxy);
    }

    @Override
    public CallbackCompletion<Boolean> delete(final String key) {
        return this.proxy.delete(key);
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        this.logger.trace("GenericKeyValueStoreConnector destroyed.");
        return this.proxy.destroy();
    }

    @Override
    public CallbackCompletion<D> get(final String key) {
        return this.proxy.get(key);
    }

    @Override
    public CallbackCompletion<List<String>> list() {
        return this.proxy.list();
    }

    @Override
    public CallbackCompletion<Boolean> set(final String key, final D data) {
        return this.proxy.set(key, data);
    }
}
