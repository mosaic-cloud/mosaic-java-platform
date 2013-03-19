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


import eu.mosaic_cloud.cloudlets.connectors.components.ComponentAcquireSucceededCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.components.ComponentCallSucceededCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.components.ComponentRequestFailedCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.components.IComponentConnectorCallbacks;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultComponentConnectorCallback<TContext, TExtra>
		extends DefaultConnectorCallback<TContext>
		implements
			IComponentConnectorCallbacks<TContext, TExtra>
{
	@Override
	public CallbackCompletion<Void> acquireFailed (final TContext context, final ComponentRequestFailedCallbackArguments<TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Resources Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> acquireSucceeded (final TContext context, final ComponentAcquireSucceededCallbackArguments<TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Resources Succeeded", true, false);
	}
	
	@Override
	public CallbackCompletion<Void> callFailed (final TContext context, final ComponentRequestFailedCallbackArguments<TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Call Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> callSucceeded (final TContext context, final ComponentCallSucceededCallbackArguments<?, TExtra> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Call Succeeded", true, false);
	}
}
