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

package eu.mosaic_cloud.cloudlets.connectors.kvstore.generic;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;

/**
 * Generic cloudlet-level connector for key value storages. Cloudlets will use
 * an object of this type to get access to a key-value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TContext>
 *            the type of the context of the cloudlet using the connector
 * @param <TValue>
 *            the type of the values exchanged with the key-value store using
 *            this connector
 * @param <TExtra>
 *            the type of the extra data; as an example, this data can be used
 *            correlation
 */
public class GenericKvStoreConnector<TContext, TValue, TExtra>
        extends
        BaseKvStoreConnector<eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TValue>, IKvStoreConnectorCallback<TContext, TValue, TExtra>, TContext, TValue, TExtra> {

    public GenericKvStoreConnector(
            final ICloudletController<?> cloudlet,
            final eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TValue> connector,
            final IConfiguration config,
            final IKvStoreConnectorCallback<TContext, TValue, TExtra> callback,
            final TContext context) {
        super(cloudlet, connector, config, callback, context);
        // FIXME
        this.initialize();
    }
}
