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

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.resources.amqp.IAmqpQueueConsumerCallback;

/**
 * Default AMQP consumer callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 * @param <D>
 *            the type of consumed data
 */
public class DefaultAmqpConsumerCallback<C, D> extends
		DefaultAmqpAccessorCallback<C> implements
		IAmqpQueueConsumerCallback<C, D> {

	@Override
	public void acknowledgeSucceeded(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Succeeded", true,
				false);
	}

	@Override
	public void acknowledgeFailed(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Failed", false,
				false);
	}

	@Override
	public void consume(C context,
			AmqpQueueConsumeCallbackArguments<C, D> arguments) {
		this.handleUnhandledCallback(arguments, "Consume", true, false);
	}

}
