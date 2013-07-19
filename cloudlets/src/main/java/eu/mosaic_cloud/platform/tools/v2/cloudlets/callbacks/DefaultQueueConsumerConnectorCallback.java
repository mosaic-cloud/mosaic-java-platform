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


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueDeliveryToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultQueueConsumerConnectorCallback<TContext, TMessage, TExtra>
			extends DefaultQueueConnectorCallback<TContext>
			implements
				QueueConsumerConnectorCallback<TContext, TMessage, TExtra>
{
	public DefaultQueueConsumerConnectorCallback () {
		super ();
	}
	
	@Override
	public CallbackCompletion<Void> acknowledgeFailed (final TContext context, final AcknowledgeFailedArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.acknowledgeFailed (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (QueueConsumerConnectorCallback.class, "acknowledgeFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> acknowledgeSucceeded (final TContext context, final AcknowledgeSucceededArguments<TExtra> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.acknowledgeSucceeded (context, arguments.extra);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (QueueConsumerConnectorCallback.class, "acknowledgeSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> consume (final TContext context, final ConsumeArguments<TMessage> arguments) {
		this.enforceCallbackArguments (context, arguments);
		final CallbackCompletion<Void> maybeCompleted = this.consume (context, arguments.message, arguments.token);
		if (maybeCompleted != DefaultCallback.NotImplemented)
			return (maybeCompleted);
		return (this.handleUnhandledCallback (QueueConsumerConnectorCallback.class, "consume", context, arguments, false, false));
	}
	
	protected CallbackCompletion<Void> acknowledgeFailed (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> acknowledgeSucceeded (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TExtra extra) {
		return (DefaultCallback.NotImplemented);
	}
	
	protected CallbackCompletion<Void> consume (@SuppressWarnings ("unused") final TContext context, @SuppressWarnings ("unused") final TMessage message, @SuppressWarnings ("unused") final QueueDeliveryToken token) {
		return (DefaultCallback.NotImplemented);
	}
}
