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

/**
 * Default AMQP publisher callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 * @param <D>
 *            the type of published data
 */
public class DefaultAmqpPublisherCallback<S, D> extends
		DefaultAmqpAccessorCallback<S> implements
		IAmqpQueuePublisherCallback<S, D> {

	@Override
	public void publishSucceeded(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Publish Succeeded", true,
				false);

	}

	@Override
	public void publishFailed(S state,
			AmqpQueuePublishCallbackArguments<S, D> arguments) {
		this.handleUnhandledCallback(arguments, "Publish Failed", false, false);

	}

}
