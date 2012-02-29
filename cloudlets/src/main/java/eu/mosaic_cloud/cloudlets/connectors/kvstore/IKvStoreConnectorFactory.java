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

package eu.mosaic_cloud.cloudlets.connectors.kvstore;

import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

public interface IKvStoreConnectorFactory extends IConnectorFactory<IKvStoreConnector<?, ?, ?>> {

    <Context, Value, Extra> IKvStoreConnector<Context, Value, Extra> create(
            IConfiguration configuration, Class<Value> valueClass,
            DataEncoder<? super Value> valueEncoder,
            IKvStoreConnectorCallback<Context, Value, Extra> callback, Context callbackContext);
}
