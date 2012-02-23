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

import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

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
public interface IKvStoreConnectorCallback<C, D, E> extends
		IConnectorCallback<C> {

	/**
	 * Called when the set operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> setSucceeded(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);

	/**
	 * Called when the set operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> setFailed(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);

	/**
	 * Called when the get operation completed successfully. The result of the
	 * get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> getSucceeded(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);

	/**
	 * Called when the get operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> getFailed(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);

	/**
	 * Called when the delete operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> deleteSucceeded(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);

	/**
	 * Called when the delete operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> deleteFailed(C context, KvStoreCallbackCompletionArguments<C, D, E> arguments);
	
	/**
	 * Called when the list operation completed successfully. The result of the
	 * get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> listSucceeded(C context, KvStoreCallbackCompletionArguments<C, List<String>, E> arguments);

	/**
	 * Called when the list operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> listFailed(C context, KvStoreCallbackCompletionArguments<C, List<String>, E> arguments);
}
