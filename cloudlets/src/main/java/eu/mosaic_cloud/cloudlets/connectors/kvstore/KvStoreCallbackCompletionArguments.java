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

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlets.core.CallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;

/**
 * The arguments of the cloudlet callback methods for the operations on
 * key-value storages.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the context of the cloudlet
 */
public class KvStoreCallbackCompletionArguments<C, D, E> extends CallbackCompletionArguments<C> {

	private final List<String> keys;
	private final D value;
	private final E extra;

	/**
	 * Creates a new argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param key
	 *            the key used in the operation
	 * @param value
	 *            the value associated with the key (if this callback is used
	 *            for failed operations this value should contain the error)
	 * @param extra
	 *            some application specific object
	 */
	public KvStoreCallbackCompletionArguments(ICloudletController<C> cloudlet,
			String key, D value, E extra) {
		super(cloudlet);
		this.keys = new ArrayList<String>();
		this.keys.add(key);
		this.value = value;
		this.extra = extra;
	}

	/**
	 * Creates a new argument for the callbacks of operations using more than
	 * one key.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param keys
	 *            the keys used in the operation
	 * @param value
	 *            the value associated with the key (if this callback is used
	 *            for failed operations this value should contain the error)
	 * @param extra
	 *            some application specific object
	 */
	public KvStoreCallbackCompletionArguments(ICloudletController<C> cloudlet,
			List<String> keys, D value, E extra) {
		super(cloudlet);
		this.keys = keys;
		this.value = value;
		this.extra = extra;
	}

	/**
	 * Returns the value field of the argument.
	 * 
	 * @return the value field of the argument
	 */
	public D getValue() {
		return this.value;
	}

	/**
	 * Returns the key used in single-key operations.
	 * 
	 * @return the key used in single-key operations
	 */
	public String getKey() {
		return this.keys.get(0);
	}

	/**
	 * Returns the keys used in multiple-key operations.
	 * 
	 * @return the key used in multiple-key operations
	 */
	public List<String> getKeys() {
		return this.keys;
	}

	/**
	 * Returns any application specific data used for the key-value store
	 * operation.
	 * 
	 * @return ny application specific data used for the key-value store
	 *         operation
	 */
	public E getExtra() {
		return this.extra;
	}

}
