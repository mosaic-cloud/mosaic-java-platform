/*
 * #%L
 * mosaic-cloudlet
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.cloudlet.resources.amqp;

import mosaic.driver.queue.amqp.AmqpOutboundMessage;

/**
 * An object of this class embeds the essential information about a publish
 * request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <D>
 *            the type of the data in the consumed message
 */
public class AmqpQueuePublishMessage<D extends Object> {
	private final AmqpQueuePublisher<?, D> publisher;
	private final AmqpOutboundMessage message;
	private final Object token;

	public AmqpQueuePublishMessage(AmqpQueuePublisher<?, D> publisher,
			AmqpOutboundMessage message, Object token) {
		super();
		this.publisher = publisher;
		this.message = message;
		this.token = token;
	}

	/**
	 * Returns the publisher object.
	 * 
	 * @return the publisher object
	 */
	public AmqpQueuePublisher<?, D> getPublisher() {
		return this.publisher;
	}

	AmqpOutboundMessage getMessage() {
		return this.message;
	}

	/**
	 * Returns the additional information required for publishing the message.
	 * This can be an identifier of the published message or anything the
	 * publisher wants to add to the message. This part will not be acttually
	 * published.
	 * 
	 * @return the additional information required for publishing the message
	 */
	public Object getToken() {
		return this.token;
	}

}
