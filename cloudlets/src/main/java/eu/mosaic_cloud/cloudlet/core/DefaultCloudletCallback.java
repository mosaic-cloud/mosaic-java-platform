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
package eu.mosaic_cloud.cloudlet.core;

/**
 * Default cloudlet callback. This extends the {@link DefaultCallback} and
 * handles callbacks for operations defined in the {@link ICloudlet}.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public abstract class DefaultCloudletCallback<S> extends DefaultCallback<S>
		implements ICloudletCallback<S> {

	@Override
	public void initializeSucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments,
				"Cloudlet Initialize Succeeded", true, true);

	}

	@Override
	public void initializeFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Initialize Failed",
				false, true);

	}

	@Override
	public void destroySucceeded(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Destroy Succeeded",
				true, false);

	}

	@Override
	public void destroyFailed(S state, CallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Cloudlet Destroy Failed",
				false, false);

	}

}
