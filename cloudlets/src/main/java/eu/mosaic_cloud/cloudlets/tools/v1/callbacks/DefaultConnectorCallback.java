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
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultConnectorCallback<TContext>
			extends DefaultCallback<TContext>
			implements
				ConnectorCallback<TContext>
{
	public DefaultConnectorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> destroyFailed (final TContext context, final CallbackArguments arguments) {
		return (this.handleUnhandledCallback (ConnectorCallback.class, "destroyFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> destroySucceeded (final TContext context, final CallbackArguments arguments) {
		return (this.handleUnhandledCallback (ConnectorCallback.class, "destroySucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> initializeFailed (final TContext context, final CallbackArguments arguments) {
		return (this.handleUnhandledCallback (ConnectorCallback.class, "initializeFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> initializeSucceeded (final TContext context, final CallbackArguments arguments) {
		return (this.handleUnhandledCallback (ConnectorCallback.class, "initializeSucceeded", context, arguments, true, false));
	}
}
