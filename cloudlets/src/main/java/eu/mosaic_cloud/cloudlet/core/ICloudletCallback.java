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
package eu.mosaic_cloud.cloudlet.core;

/**
 * Main interface for user cloudlets. All user cloudlets must implement this
 * interface.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            The type of the object encoding the state of the cloudlet.
 */
public interface ICloudletCallback<S> extends ICallback {

	/**
	 * Initializes the user cloudlet.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initialize(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is successfully initialized.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initializeSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is unsuccessfully initialized.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initializeFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Destrozs the user cloudlet.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroy(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is successfully destroyed.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroySucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is unsuccessfully destroyed.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroyFailed(S state, CallbackArguments<S> arguments);

}
