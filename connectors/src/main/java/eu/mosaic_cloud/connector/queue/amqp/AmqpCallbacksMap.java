/*
 * #%L
 * mosaic-connector
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
package eu.mosaic_cloud.connector.queue.amqp;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a Map between consumer identifiers and consume callbacks.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpCallbacksMap {

	private Map<String, IAmqpConsumerCallback> handlerMap = new HashMap<String, IAmqpConsumerCallback>();

	public AmqpCallbacksMap() {
		super();
	}

	/**
	 * Add callback for the consumer identified with the given identifier. If
	 * another callback has been added previously for the consumer, this
	 * callback will be replaced.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @param callback
	 *            the callback
	 */
	public void addHandlers(String consumerId, IAmqpConsumerCallback callback) {
		this.handlerMap.put(consumerId, callback);
	}

	/**
	 * Removes from the map the callback for a consumer and the actual consumer.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @return the callback
	 */
	public IAmqpConsumerCallback removeConsumerCallback(String consumerId) {
		return this.handlerMap.remove(consumerId);
	}

	/**
	 * Returns the callback for a consumer and the actual consumer.
	 * 
	 * @param consumerId
	 *            the consumer identifier
	 * @return the callback
	 */
	public IAmqpConsumerCallback getRequestHandlers(String consumerId) {
		return this.handlerMap.get(consumerId);
	}
}
