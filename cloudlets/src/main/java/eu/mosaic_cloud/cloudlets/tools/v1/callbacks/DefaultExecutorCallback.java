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
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.ExecutorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultExecutorCallback<TContext, TOutcome, TExtra>
			extends DefaultConnectorCallback<TContext>
			implements
				ExecutorCallback<TContext, TOutcome, TExtra>
{
	public DefaultExecutorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> executionFailed (final TContext context, final ExecutionFailedArguments<TOutcome, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.executionFailed (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (ExecutorCallback.class, "executionFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> executionSucceeded (final TContext context, final ExecutionSucceededArguments<TOutcome, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.executionSucceeded (context, arguments.outcome, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (ExecutorCallback.class, "executionSucceeded", context, arguments, true, false));
	}
	
	protected CallbackCompletion<Void> executionFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> executionSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TOutcome outcome, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
}
