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
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultAmqpQueuePublisherConnectorCallback<TContext, TValue, TExtra>
			extends DefaultAmqpQueueConnectorCallback<TContext>
			implements
				AmqpQueuePublisherConnectorCallback<TContext, TValue, TExtra>
{
	public DefaultAmqpQueuePublisherConnectorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> publishFailed (final TContext context, final PublishFailedArguments<TValue, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.publishFailed (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.callbackNotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (AmqpQueuePublisherConnectorCallback.class, "publishFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> publishSucceeded (final TContext context, final PublishSucceededArguments<TValue, TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.publishSucceeded (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.callbackNotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (AmqpQueuePublisherConnectorCallback.class, "publishSucceeded", context, arguments, true, false));
	}
	
	protected CallbackCompletion<Void> publishFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.callbackNotImplemented);
	}
	
	protected CallbackCompletion<Void> publishSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.callbackNotImplemented);
	}
}
