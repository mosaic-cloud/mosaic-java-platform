/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.cloudlet.resources.kvstore;

/**
 * Interface for memcached-based key-value storage accessor callbacks. This
 * interface should be implemented directly or indirectly by cloudlets wishing
 * to use a memcached-based key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IMemcachedAccessorCallback<S> extends
		IKeyValueAccessorCallback<S> {
	/**
	 * Called when the add operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void addSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the add operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void addFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the append operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void appendSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the append operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void appendFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the prepend operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void prependSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the prepend operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void prependFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the replace operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void replaceSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the replace operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void replaceFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the getBulk operation completed successfully. The result of
	 * the get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the getBulk operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkFailed(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the cas operation completed successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void casSucceeded(S state, KeyValueCallbackArguments<S> arguments);

	/**
	 * Called when the cas operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void casFailed(S state, KeyValueCallbackArguments<S> arguments);
}
