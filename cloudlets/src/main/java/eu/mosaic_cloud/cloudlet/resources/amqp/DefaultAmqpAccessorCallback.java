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

import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.DefaultResourceAccessorCallback;

/**
 * Default AMQP resource accessor callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultAmqpAccessorCallback<S> extends
		DefaultResourceAccessorCallback<S> implements
		IAmqpQueueAccessorCallback<S> {

	@Override
	public void registerSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Register Succeeded", true,
				false);
	}

	@Override
	public void registerFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Register Failed", false, true);
	}

	@Override
	public void unregisterSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Succeeded", true,
				false);
	}

	@Override
	public void unregisterFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Unregister Failed", false,
				true);
	}

}
