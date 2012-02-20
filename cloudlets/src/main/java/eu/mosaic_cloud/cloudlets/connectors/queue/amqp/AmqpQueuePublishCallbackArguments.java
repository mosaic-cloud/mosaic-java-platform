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

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;

/**
 * The arguments of the cloudlet callback methods for the publish request.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the context of the cloudlet
 * @param <D>
 *            the type of the published data
 */
public class AmqpQueuePublishCallbackArguments<C, D> extends
		CallbackArguments<C> {

	private AmqpQueuePublishMessage<D> message;

	/**
	 * Creates a new callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param message
	 *            information about the publish request
	 */
	public AmqpQueuePublishCallbackArguments(ICloudletController<C> cloudlet,
			AmqpQueuePublishMessage<D> message) {
		super(cloudlet);
		this.message = message;
	}

	/**
	 * Returns information about the publish request.
	 * 
	 * @return information about the publish request
	 */
	public AmqpQueuePublishMessage<D> getMessage() {
		return this.message;
	}

}
