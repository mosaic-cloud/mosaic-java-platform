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

/**
 * Interface for registering and using for an AMQP resource as a consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this accessor
 * @param <D>
 *            the type of the consumed data
 */
public interface IAmqpQueueConsumerConnector<C, D> extends IAmqpQueueConnector<C> {

	/**
	 * Acknowledges a message.
	 * 
	 * @param message
	 *            the message to acknowledge
	 */
	public void acknowledge(AmqpQueueConsumeMessage<D> message);
}
