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

/**
 * Interface for memcached-based key-value storage accessor callbacks. This
 * interface should be implemented directly or indirectly by cloudlets wishing
 * to use a memcached-based key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the cloudlet context
 */
public interface IMemcacheKvStoreConnectorCallback<C> extends
		IKvStoreConnectorCallback<C> {

	/**
	 * Called when the add operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void addSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the add operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void addFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the append operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void appendSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the append operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void appendFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the prepend operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void prependSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the prepend operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void prependFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the replace operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void replaceSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the replace operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void replaceFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the getBulk operation completed successfully. The result of
	 * the get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the getBulk operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void getBulkFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the cas operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void casSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the cas operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void casFailed(C context, KeyValueCallbackArguments<C> arguments);
}
