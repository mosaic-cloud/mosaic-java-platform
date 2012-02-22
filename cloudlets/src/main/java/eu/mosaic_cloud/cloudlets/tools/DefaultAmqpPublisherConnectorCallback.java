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

package eu.mosaic_cloud.cloudlets.tools;

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublishCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Default AMQP publisher callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 * @param <D>
 *            the type of published data
 */
public class DefaultAmqpPublisherConnectorCallback<C, D>
		extends DefaultAmqpQueueConnectorCallback<C>
		implements
			IAmqpQueuePublisherConnectorCallback<C, D>
{
	@Override
	public CallbackCompletion<Void> publishFailed (final C context, final AmqpQueuePublishCallbackCompletionArguments<C, D> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Publish Failed", false, false);
	}
	
	@Override
	public CallbackCompletion<Void> publishSucceeded (final C context, final AmqpQueuePublishCallbackCompletionArguments<C, D> arguments)
	{
		return this.handleUnhandledCallback (arguments, "Publish Succeeded", true, false);
	}
}
