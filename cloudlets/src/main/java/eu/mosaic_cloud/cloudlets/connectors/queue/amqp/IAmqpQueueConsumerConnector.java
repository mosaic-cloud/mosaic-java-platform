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

package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.connectors.queue.amqp.IAmqpMessageToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Interface for registering and using for an AMQP resource as a consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TMessage>
 *            the type of the consumed data
 * @param <TExtra>
 *            the type of the extra data; as an example, this data can be used
 *            correlation
 */
public interface IAmqpQueueConsumerConnector<TMessage, TExtra>
		extends
			IAmqpQueueConnector,
			eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<TMessage>
{
	/**
	 * Acknowledges a message.
	 * 
	 * @param token
	 *            the delivery token in the received message
	 * @param extra
	 */
	CallbackCompletion<Void> acknowledge (IAmqpMessageToken token, TExtra extra);
}
