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

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;

/**
 * Base interface for key-value storage accessor callbacks. This interface
 * should be implemented directly or indirectly by cloudlets wishing to use a
 * key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the cloudlet context
 */
public interface IKvStoreConnectorCallback<C> extends
		IConnectorCallback<C> {

	/**
	 * Called when the set operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void setSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the set operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void setFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the get operation completed successfully. The result of the
	 * get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void getSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the get operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void getFailed(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the delete operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void deleteSucceeded(C context, KeyValueCallbackArguments<C> arguments);

	/**
	 * Called when the delete operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	void deleteFailed(C context, KeyValueCallbackArguments<C> arguments);
}