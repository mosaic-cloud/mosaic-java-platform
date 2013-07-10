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

package eu.mosaic_cloud.cloudlets.tools.v1.callbacks;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultKvStoreConnectorCallback<TContext, TValue, TExtra>
			extends DefaultConnectorCallback<TContext>
			implements
				KvStoreConnectorCallback<TContext, TValue, TExtra>
{
	public DefaultKvStoreConnectorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> deleteFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "deleteFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> deleteSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "deleteSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> getFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "getFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> getSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "getSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> setFailed (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "setFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> setSucceeded (final TContext context, final KvStoreCallbackCompletionArguments<TValue, TExtra> arguments) {
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "setSucceeded", context, arguments, true, false));
	}
}
