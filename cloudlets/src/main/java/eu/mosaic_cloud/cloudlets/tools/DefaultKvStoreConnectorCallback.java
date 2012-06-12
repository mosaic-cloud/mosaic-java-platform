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

package eu.mosaic_cloud.cloudlets.tools;


import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Default key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TContext>
 *            the type of the context of the cloudlet using this callback
 * @param <TValue>
 *            the type of the values exchanged with the key-value store using
 *            this connector
 * @param <TExtra>
 *            the type of the extra data; as an example, this data can be used
 *            correlation
 */
public class DefaultKvStoreConnectorCallback<TContext, TValue, TExtra extends MessageEnvelope>
		extends DefaultConnectorCallback<TContext>
		implements
			IKvStoreConnectorCallback<TContext, TValue, TExtra>
{
	@Override
	public CallbackCompletion<Void> deleteFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Delete Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> deleteSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Delete Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> getFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Get Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> getSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Get Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> listFailed (final TContext context, final KvStoreCallbackCompletionArguments<List<String>, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> listSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<List<String>, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> setFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> setSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Succeeded", true, false);
	}
}
