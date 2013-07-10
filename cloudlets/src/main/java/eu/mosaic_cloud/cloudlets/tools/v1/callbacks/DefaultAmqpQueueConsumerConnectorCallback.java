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
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class DefaultAmqpQueueConsumerConnectorCallback<TContext, TValue, TExtra>
			extends DefaultAmqpQueueConnectorCallback<TContext>
			implements
				AmqpQueueConsumerConnectorCallback<TContext, TValue, TExtra>
{
	public DefaultAmqpQueueConsumerConnectorCallback (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledgeFailed (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments) {
		return (this.handleUnhandledCallback (AmqpQueueConsumerConnectorCallback.class, "acknowledgeFailed", context, arguments, false, false));
	}
	
	@Override
	public CallbackCompletion<Void> acknowledgeSucceeded (final TContext context, final GenericCallbackCompletionArguments<TExtra> arguments) {
		return (this.handleUnhandledCallback (AmqpQueueConsumerConnectorCallback.class, "acknowledgeSucceeded", context, arguments, true, false));
	}
	
	@Override
	public CallbackCompletion<Void> consume (final TContext context, final AmqpQueueConsumeCallbackArguments<TValue> arguments) {
		return (this.handleUnhandledCallback (AmqpQueueConsumerConnectorCallback.class, "consume", context, arguments, false, false));
	}
}
