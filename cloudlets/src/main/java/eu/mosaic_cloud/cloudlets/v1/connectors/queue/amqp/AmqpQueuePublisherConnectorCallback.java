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

package eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface AmqpQueuePublisherConnectorCallback<TContext extends Object, TMessage extends Object, TExtra extends Object>
			extends
				AmqpQueueConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> publishFailed (TContext context, PublishFailedArguments<TMessage, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> publishSucceeded (TContext context, PublishSucceededArguments<TMessage, TExtra> arguments);
	
	public static final class PublishFailedArguments<TMessage extends Object, TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public PublishFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final TMessage message, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.message = message;
		}
		
		public final TMessage message;
	}
	
	public static final class PublishSucceededArguments<TMessage extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public PublishSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final TMessage message, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.message = message;
		}
		
		public final TMessage message;
	}
}
