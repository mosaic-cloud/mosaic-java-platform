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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback.PublishFailedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback.PublishSucceededArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public class AmqpQueuePublisherConnector<TContext, TMessage, TExtra>
			extends BaseAmqpQueueConnector<eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueuePublisherConnector<TMessage>, AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra>, TContext>
			implements
				eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnector<TMessage, TExtra>
{
	public AmqpQueuePublisherConnector (final CloudletController<?> cloudlet, final eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueuePublisherConnector<TMessage> connector, final Configuration configuration, final AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext context) {
		super (cloudlet, connector, configuration, callback, context);
	}
	
	@Override
	public CallbackCompletion<Void> publish (final TMessage message) {
		return this.publish (message, null);
	}
	
	@Override
	public CallbackCompletion<Void> publish (final TMessage message, final TExtra extra) {
		final CallbackCompletion<Void> completion = this.connector.publish (message);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
					assert (completion_ == completion);
					if (completion.getException () != null) {
						return AmqpQueuePublisherConnector.this.callback.publishFailed (AmqpQueuePublisherConnector.this.context, new PublishFailedArguments<TMessage, TExtra> (AmqpQueuePublisherConnector.this.cloudlet, AmqpQueuePublisherConnector.this, message, completion.getException (), extra));
					}
					return AmqpQueuePublisherConnector.this.callback.publishSucceeded (AmqpQueuePublisherConnector.this.context, new PublishSucceededArguments<TMessage, TExtra> (AmqpQueuePublisherConnector.this.cloudlet, AmqpQueuePublisherConnector.this, message, extra));
				}
			});
		}
		return completion;
	}
}
