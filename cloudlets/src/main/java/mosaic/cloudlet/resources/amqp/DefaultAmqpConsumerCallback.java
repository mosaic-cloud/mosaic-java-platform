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

import mosaic.cloudlet.core.CallbackArguments;

/**
 * Default AMQP consumer callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 * @param <D>
 *            the type of consumed data
 */
public class DefaultAmqpConsumerCallback<S, D> extends
		DefaultAmqpAccessorCallback<S> implements
		IAmqpQueueConsumerCallback<S, D> {

	@Override
	public void acknowledgeSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Succeeded", true,
				false);
	}

	@Override
	public void acknowledgeFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Acknowledge Failed", false,
				false);
	}

	@Override
	public void consume(S state,
			AmqpQueueConsumeCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Consume", true, false);
	}

}
