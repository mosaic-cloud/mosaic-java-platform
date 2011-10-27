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
import mosaic.cloudlet.resources.IResourceAccessorCallback;

/**
 * Basic interface for AMQP accessor callbacks.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IAmqpQueueAccessorCallback<S> extends
		IResourceAccessorCallback<S> {
	/**
	 * Called when consumer or publisher registered successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void registerSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher failed to register.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void registerFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher unregistered successfully.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void unregisterSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Called when consumer or publisher failed to unregister.
	 * 
	 * @param state
	 *            cloudlet state
	 * @param arguments
	 *            callback arguments
	 */
	void unregisterFailed(S state, CallbackArguments<S> arguments);
}
