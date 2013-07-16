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


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public abstract class DefaultCloudletCallback<TContext>
			extends DefaultCallback<TContext>
			implements
				CloudletCallback<TContext>
{
	public DefaultCloudletCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> destroyFailed (final TContext context, final DestroyFailedArguments arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.destroyFailed (context, arguments.error);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (CloudletCallback.class, "destroyFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> destroySucceeded (final TContext context, final DestroySucceededArguments arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.destroySucceeded (context);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (CloudletCallback.class, "destroySucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> initializeFailed (final TContext context, final InitializeFailedArguments arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.initializeFailed (context, arguments.error);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (CloudletCallback.class, "initializeFailed", context, arguments, false, true));
	}
	
	@Override
	public CallbackCompletion<Void> initializeSucceeded (final TContext context, final InitializeSucceededArguments arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.initializeSucceeded (context);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (CloudletCallback.class, "initializeSucceeded", context, arguments, true, false));
	}
	
	protected CallbackCompletion<Void> destroy (@SuppressWarnings ("unused") final TContext context) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> destroyFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> destroySucceeded (@SuppressWarnings ("unused") final TContext context) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> initialize (@SuppressWarnings ("unused") final TContext context) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> initializeFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> initializeSucceeded (@SuppressWarnings ("unused") final TContext context) {
		return (DefaultCallback.NotImplemented);
	}
}
