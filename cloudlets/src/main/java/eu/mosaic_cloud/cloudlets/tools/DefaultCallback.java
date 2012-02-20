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
import eu.mosaic_cloud.cloudlets.core.ICallback;

import eu.mosaic_cloud.platform.core.log.MosaicLogger;

/**
 * Default callback class.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultCallback<C> implements ICallback {
	protected MosaicLogger logger = MosaicLogger.createLogger(this);

	/**
	 * Handles any unhandled callback.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful
	 *            termination of the operation
	 * @param couldDestroy
	 *            <code>true</code> if cloudlet can be destroyed here
	 */
	protected void handleUnhandledCallback(CallbackArguments<C> arguments,
			String callbackType, boolean positive, boolean couldDestroy) {
		this.traceUnhandledCallback(arguments, callbackType, positive);
		if (!positive && couldDestroy) {
			arguments.getCloudlet().destroy();
		}
	}

	/**
	 * Traces unhandled callbacks.
	 * 
	 * @param arguments
	 *            the arguments of the callback
	 * @param callbackType
	 *            a string describing the type of callback (e.g. initialize)
	 * @param positive
	 *            <code>true</code> if callback corresponds to successful
	 *            termination of the operation
	 */
	protected void traceUnhandledCallback(CallbackArguments<C> arguments,
			String callbackType, boolean positive) {
		logger.info("unhandled cloudlet callback: `"
				+ this.getClass().getName() + "`@`" + callbackType + "` "
				+ (positive ? "Succeeded" : "Failed"));
	}
}
