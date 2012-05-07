/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpMessageToken;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public class AmqpQueueConsumerConnector<TContext, TMessage, TExtra>
		extends BaseAmqpQueueConnector<eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<TMessage>, IAmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra>, TContext>
		implements
			IAmqpQueueConsumerConnector<TMessage, TExtra>
{
	@SuppressWarnings ("synthetic-access")
	public AmqpQueueConsumerConnector (final ICloudletController<?> cloudlet, final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<TMessage> connector, final IConfiguration configuration, final IAmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext context, final Callback<TMessage> backingCallback)
	{
		super (cloudlet, connector, configuration, callback, context);
		backingCallback.connector = this;
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final IAmqpMessageToken token)
	{
		return this.acknowledge (token, null);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final IAmqpMessageToken token, final TExtra extra)
	{
		final CallbackCompletion<Void> completion = this.connector.acknowledge (token);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion); // NOPMD
					CallbackCompletion<Void> result;
					if (completion.getException () == null) {
						result = AmqpQueueConsumerConnector.this.callback.acknowledgeSucceeded (AmqpQueueConsumerConnector.this.context, new GenericCallbackCompletionArguments<TExtra> (AmqpQueueConsumerConnector.this.cloudlet, extra));
					} else {
						result = AmqpQueueConsumerConnector.this.callback.acknowledgeFailed (AmqpQueueConsumerConnector.this.context, new GenericCallbackCompletionArguments<TExtra> (AmqpQueueConsumerConnector.this.cloudlet, completion.getException ()));
					}
					return result;
				}
			});
		}
		return completion;
	}
	
	protected CallbackCompletion<Void> consume (final IAmqpMessageToken token, final TMessage message)
	{
		CallbackCompletion<Void> result;
		if (this.callback == null) {
			result = CallbackCompletion.createFailure (new IllegalStateException ());
		} else {
			result = this.callback.consume (this.context, new AmqpQueueConsumeCallbackArguments<TMessage> (this.cloudlet, token, message));
		}
		return result;
	}
	
	public static final class Callback<Message>
			implements
				IAmqpQueueConsumerCallback<Message>
	{
		@Override
		public CallbackCompletion<Void> consume (final IAmqpMessageToken token, final Message message)
		{
			return this.connector.consume (token, message);
		}
		
		private AmqpQueueConsumerConnector<?, Message, ?> connector = null; // NOPMD
	}
}
