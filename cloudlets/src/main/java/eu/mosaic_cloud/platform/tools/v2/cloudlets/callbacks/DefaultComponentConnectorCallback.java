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

package eu.mosaic_cloud.platform.tools.v2.cloudlets.callbacks;


import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.component.ComponentConnectorCallbacks;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultComponentConnectorCallback<TContext, TExtra>
			extends DefaultConnectorCallback<TContext>
			implements
				ComponentConnectorCallbacks<TContext, TExtra>
{
	public DefaultComponentConnectorCallback () {
		super ();
	}
	
	@Override
	public CallbackCompletion<Void> acquireFailed (final TContext context, final AcquireFailedArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.acquireFailed (context, arguments.error, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (DefaultComponentConnectorCallback.class, "acquireFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> acquireSucceeded (final TContext context, final AcquireSucceededArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.acquireSucceeded (context, arguments.descriptor, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (DefaultComponentConnectorCallback.class, "acquireSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> callFailed (final TContext context, final CallFailedArguments<?, ?, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.callFailed (context, arguments.error, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (DefaultComponentConnectorCallback.class, "callFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> callSucceeded (final TContext context, final CallSucceededArguments<?, ?, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.callSucceded (context, arguments.outputs, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (DefaultComponentConnectorCallback.class, "callSucceeded", context, arguments, true, false));
	}
	
	protected CallbackCompletion<Void> acquireFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> acquireSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final ComponentResourceDescriptor descriptor, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> callFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> callSucceded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Object outputs, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
}
