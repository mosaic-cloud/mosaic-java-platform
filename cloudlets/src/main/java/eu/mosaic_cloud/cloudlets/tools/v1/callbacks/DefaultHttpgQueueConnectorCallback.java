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
import eu.mosaic_cloud.cloudlets.v1.connectors.httpg.HttpgQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.httpg.HttpgQueueRequestedCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultHttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>
			extends DefaultQueueConnectorCallback<TContext>
			implements
				HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>
{
	public DefaultHttpgQueueConnectorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> requested (final TContext context, final HttpgQueueRequestedCallbackArguments<TRequestBody> arguments) {
		return (this.handleUnhandledCallback (HttpgQueueConnectorCallback.class, "requested", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> respondFailed (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments) {
		return (this.handleUnhandledCallback (HttpgQueueConnectorCallback.class, "respondFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> respondSucceeded (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments) {
		return (this.handleUnhandledCallback (HttpgQueueConnectorCallback.class, "respondSucceeded", context, arguments, false, false));
	}
}
