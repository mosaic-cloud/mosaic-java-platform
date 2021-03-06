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


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultKvStoreConnectorCallback<TContext, TValue, TExtra>
			extends DefaultConnectorCallback<TContext>
			implements
				KvStoreConnectorCallback<TContext, TValue, TExtra>
{
	public DefaultKvStoreConnectorCallback () {
		super ();
	}
	
	@Override
	public CallbackCompletion<Void> deleteFailed (final TContext context, final DeleteFailedArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.deleteFailed (context, arguments.error, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "deleteFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> deleteSucceeded (final TContext context, final DeleteSucceededArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.deleteSucceeded (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "deleteSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> getFailed (final TContext context, final GetFailedArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.getFailed (context, arguments.error, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "getFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> getSucceeded (final TContext context, final GetSucceededArguments<TValue, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.getSucceeded (context, arguments.value, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "getSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> setFailed (final TContext context, final SetFailedArguments<TValue, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.setFailed (context, arguments.error, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "setFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> setSucceeded (final TContext context, final SetSucceededArguments<TValue, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.setSucceeded (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (KvStoreConnectorCallback.class, "setSucceeded", context, arguments, true, false));
	}
	
	protected CallbackCompletion<Void> deleteFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> deleteSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> getFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> getSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TValue value, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> setFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final Throwable error, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> setSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
}
