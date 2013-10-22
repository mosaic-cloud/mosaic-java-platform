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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.queue;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback.AcknowledgeFailedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback.AcknowledgeSucceededArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback.ConsumeArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerCallback;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueDeliveryToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;


public abstract class BaseQueueConsumerConnector<TContext, TMessage, TExtra>
			extends BaseQueueConnector<eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerConnector<TMessage>, QueueConsumerConnectorCallback<TContext, TMessage, TExtra>, TContext>
			implements
				eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnector<TMessage, TExtra>
{
	@SuppressWarnings ("synthetic-access")
	protected BaseQueueConsumerConnector (final CloudletController<?> cloudlet, final eu.mosaic_cloud.platform.v2.connectors.queue.QueueConsumerConnector<TMessage> connector, final ConfigurationSource configuration, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext context, final Callback<TMessage> backingCallback) {
		super (cloudlet, connector, configuration, callback, context);
		backingCallback.connector = this;
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final QueueDeliveryToken token) {
		return this.acknowledge (token, null);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final QueueDeliveryToken token, final TExtra extra) {
		final CallbackCompletion<Void> completion = this.connector.acknowledge (token);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
					assert (completion_ == completion);
					CallbackCompletion<Void> result;
					if (completion.getException () == null) {
						result = BaseQueueConsumerConnector.this.callback.acknowledgeSucceeded (BaseQueueConsumerConnector.this.context, new AcknowledgeSucceededArguments<TExtra> (BaseQueueConsumerConnector.this.cloudlet, BaseQueueConsumerConnector.this, token, extra));
					} else {
						result = BaseQueueConsumerConnector.this.callback.acknowledgeFailed (BaseQueueConsumerConnector.this.context, new AcknowledgeFailedArguments<TExtra> (BaseQueueConsumerConnector.this.cloudlet, BaseQueueConsumerConnector.this, token, completion.getException (), extra));
					}
					return result;
				}
			});
		}
		return completion;
	}
	
	protected CallbackCompletion<Void> consume (final QueueDeliveryToken token, final TMessage message) {
		CallbackCompletion<Void> result;
		if (this.callback == null) {
			result = CallbackCompletion.createFailure (new IllegalStateException ());
		} else {
			result = this.callback.consume (this.context, new ConsumeArguments<TMessage> (this.cloudlet, this, message, token));
		}
		return result;
	}
	
	public static class Callback<Message>
				implements
					QueueConsumerCallback<Message>
	{
		@Override
		public CallbackCompletion<Void> consume (final QueueDeliveryToken token, final Message message) {
			return this.connector.consume (token, message);
		}
		
		private BaseQueueConsumerConnector<?, Message, ?> connector = null;
	}
}
