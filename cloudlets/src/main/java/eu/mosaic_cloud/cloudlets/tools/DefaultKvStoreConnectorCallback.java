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
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Default key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultKvStoreConnectorCallback<C, D, E>
		extends DefaultConnectorCallback<C>
		implements
			IKvStoreConnectorCallback<C, D, E>
{
	@Override
	public CallbackCompletion<Void> deleteFailed (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Delete Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> deleteSucceeded (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Delete Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> getFailed (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Get Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> getSucceeded (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Get Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> setFailed (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> setSucceeded (final C context, final KvStoreCallbackCompletionArguments<C, D, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Succeeded", true, false);
	}

	@Override
	public CallbackCompletion<Void> listFailed (final C context, final KvStoreCallbackCompletionArguments<C, List<String>, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> listSucceeded (final C context, final KvStoreCallbackCompletionArguments<C, List<String>, E> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Set Succeeded", true, false);
	}
}
