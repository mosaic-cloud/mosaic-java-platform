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

package eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.Connector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorCallbackArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueueDeliveryToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface QueueConsumerConnectorCallback<TContext extends Object, TMessage extends Object, TExtra extends Object>
			extends
				QueueConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> acknowledgeFailed (TContext context, AcknowledgeFailedArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> acknowledgeSucceeded (TContext context, AcknowledgeSucceededArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> consume (TContext context, ConsumeArguments<TMessage> arguments);
	
	public static final class AcknowledgeFailedArguments<TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public AcknowledgeFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final QueueDeliveryToken token, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.token = token;
		}
		
		public final QueueDeliveryToken token;
	}
	
	public static final class AcknowledgeSucceededArguments<TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public AcknowledgeSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final QueueDeliveryToken token, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.token = token;
		}
		
		public final QueueDeliveryToken token;
	}
	
	public static final class ConsumeArguments<TMessage extends Object>
				extends ConnectorCallbackArguments
	{
		public ConsumeArguments (final CloudletController<?> cloudlet, final Connector connector, final TMessage message, final QueueDeliveryToken token) {
			super (cloudlet, connector);
			this.message = message;
			this.token = token;
		}
		
		public final TMessage message;
		public final QueueDeliveryToken token;
	}
}
