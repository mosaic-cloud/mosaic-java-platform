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
 * Interface for AMQP queue publishers. This will be implemented by cloudlets
 * which need to send messages to an exchange.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 * @param <D>
 *            the type of published data
 */
public interface IAmqpQueuePublisherCallback<S, D> extends
		IAmqpQueueAccessorCallback<S> {
	/**
	 * Called when the publisher receives confirmation that the message
	 * publishing finished successfully.
	 * 
	 * @param <D>
	 *            the type of the published message
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void publishSucceeded(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments);

	/**
	 * Called when the publisher receives notification that the message
	 * publishing could not be finished with success.
	 * 
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            the arguments of the callback
	 */
	void publishFailed(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments);
}
