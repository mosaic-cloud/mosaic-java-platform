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

import eu.mosaic_cloud.drivers.queue.amqp.AmqpOutboundMessage;

/**
 * An object of this class embeds the essential information about a publish
 * request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Context>
 *            the type of the data in the consumed message
 */
public class AmqpQueuePublishMessage<Context> {

	private final AmqpQueuePublisherConnector<?, Context> publisher;
	private final AmqpOutboundMessage message;

	public AmqpQueuePublishMessage(AmqpQueuePublisherConnector<?, Context> publisher,
			AmqpOutboundMessage message) {
		super();
		this.publisher = publisher;
		this.message = message;
	}

	/**
	 * Returns the publisher object.
	 * 
	 * @return the publisher object
	 */
	public AmqpQueuePublisherConnector<?, Context> getPublisher() {
		return this.publisher;
	}

	AmqpOutboundMessage getMessage() {
		return this.message;
	}
}
