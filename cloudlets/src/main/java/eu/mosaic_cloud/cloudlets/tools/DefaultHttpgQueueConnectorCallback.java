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

package eu.mosaic_cloud.cloudlets.tools;


import eu.mosaic_cloud.cloudlets.connectors.httpg.HttpgQueueRequestedCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.httpg.IHttpgQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultHttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>
		extends DefaultQueueConnectorCallback<TContext>
		implements
			IHttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>
{
	@Override
	public CallbackCompletion<Void> requested (final TContext context, final HttpgQueueRequestedCallbackArguments<TRequestBody> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Requested", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> respondFailed (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Acknowledge Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> respondSucceeded (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Acknowledge Succeeded", true, false);
	}
}
