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

import eu.mosaic_cloud.cloudlets.connectors.queue.IQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Basic interface for AMQP accessor callbacks.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the cloudlet context
 */
public interface IAmqpQueueConnectorCallback<C> extends
		IQueueConnectorCallback<C> {

	/**
	 * Called when consumer or publisher registered successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> registerSucceeded(C context, CallbackArguments<C> arguments);

	/**
	 * Called when consumer or publisher failed to register.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> registerFailed(C context, CallbackArguments<C> arguments);

	/**
	 * Called when consumer or publisher unregistered successfully.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> unregisterSucceeded(C context, CallbackArguments<C> arguments);

	/**
	 * Called when consumer or publisher failed to unregister.
	 * 
	 * @param context
	 *            cloudlet context
	 * @param arguments
	 *            callback arguments
	 */
	CallbackCompletion<Void> unregisterFailed(C context, CallbackArguments<C> arguments);
}
