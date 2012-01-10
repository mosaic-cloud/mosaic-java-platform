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
package eu.mosaic_cloud.cloudlet.resources.amqp;

/**
 * Interface for registering and using for an AMQP resource as a publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this accessor
 * @param <D>
 *            the type of the published data
 */
public interface IAmqpQueuePublisher<S, D> extends IAmqpQueueAccessor<S> {
	/**
	 * Publishes a message to a queue.
	 * 
	 * @param data
	 *            the data to publish
	 * @param token
	 *            extra info specific to the published data
	 * @param contentType
	 *            the RFC-2046 MIME type for the Message content (such as
	 *            "text/plain")
	 */
	void publish(D data, Object token, String contentType);
	
	/**
	 * Publishes a message to a queue.
	 * 
	 * @param data
	 *            the data to publish
	 * @param token
	 *            extra info specific to the published data
	 * @param contentType
	 *            the RFC-2046 MIME type for the Message content (such as
	 *            "text/plain")
	 */
	void publish(D data, Object token, String contentType, String correlation);
}
