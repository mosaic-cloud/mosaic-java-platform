/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.memcache;


import java.util.Map;

import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Interface for memcached-based key-value storage accessor callbacks. This
 * interface should be implemented directly or indirectly by cloudlets wishing
 * to use a memcached-based key value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TContext>
 *            the type of the cloudlet context
 * @param <TValue>
 *            the type of the values exchanged with the key-value store using
 *            this connector
 * @param <TExtra>
 *            the type of the extra data; as an example, this data can be used
 *            correlation
 */
public interface IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra>
		extends
			IKvStoreConnectorCallback<TContext, TValue, TExtra>
{
	/**
	 * Called when the add operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> addFailed (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the add operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> addSucceeded (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the append operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> appendFailed (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the append operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> appendSucceeded (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the cas operation completed unsuccessfully. The error can be
	 * retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> casFailed (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the cas operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> casSucceeded (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the getBulk operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> getBulkFailed (TContext context, KvStoreCallbackCompletionArguments<Map<String, TValue>, TExtra> arguments);
	
	/**
	 * Called when the getBulk operation completed successfully. The result of
	 * the get operation can be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> getBulkSucceeded (TContext context, KvStoreCallbackCompletionArguments<Map<String, TValue>, TExtra> arguments);
	
	/**
	 * Called when the prepend operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> prependFailed (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the prepend operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> prependSucceeded (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the replace operation completed unsuccessfully. The error can
	 * be retrieved from the <i>arguments</i> parameter.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> replaceFailed (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
	
	/**
	 * Called when the replace operation completed successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> replaceSucceeded (TContext context, KvStoreCallbackCompletionArguments<TValue, TExtra> arguments);
}
