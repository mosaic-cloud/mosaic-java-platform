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

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;

/**
 * Default AMQP resource accessor callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultAmqpQueueConnectorCallback<C> extends
		DefaultQueueConnectorCallback<C> implements
		IAmqpQueueConnectorCallback<C> {

	@Override
	public void registerSucceeded(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Register Succeeded", true,
				false);
	}

	@Override
	public void registerFailed(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Register Failed", false, true);
	}

	@Override
	public void unregisterSucceeded(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Succeeded", true,
				false);
	}

	@Override
	public void unregisterFailed(C context, CallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Failed", false,
				true);
	}

}
