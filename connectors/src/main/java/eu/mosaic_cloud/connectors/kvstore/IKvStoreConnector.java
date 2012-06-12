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

import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Interface for working with key-value stores.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 */
public interface IKvStoreConnector<TValue extends Object, TExtra extends MessageEnvelope>
		extends
			IConnector
{
	/**
	 * Deletes the given key.
	 * 
	 * @param key
	 *            the key to delete
	 * @return a result handle for the operation
	 */
	CallbackCompletion<Void> delete (String key);
	
	/**
	 * Gets data associated with a single key.
	 * 
	 * @param key
	 *            the key
	 * @param extra
	 *            additional information needed for processing the message (e.g.
	 *            expected message encoding)
	 * @return a result handle for the operation
	 */
	CallbackCompletion<TValue> get (String key, final TExtra extra);
	
	/**
	 * Lists the keys of the bucket used by the connector.
	 * 
	 * @return a result handle for the operation
	 */
	CallbackCompletion<List<String>> list ();
	
	/**
	 * Stores the given data and associates it with the specified key.
	 * 
	 * @param key
	 *            the key under which this data should be stored
	 * @param data
	 *            the data
	 * @param extra
	 *            additional information needed for processing the message
	 * @return a result handle for the operation
	 */
	CallbackCompletion<Void> set (final String key, final TValue data, final TExtra extra);
}
