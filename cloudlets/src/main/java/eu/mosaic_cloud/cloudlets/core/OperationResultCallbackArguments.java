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
package eu.mosaic_cloud.cloudlets.core;

/**
 * Base class for clouldet callback arguments. This will hold a reference to the
 * cloudlet controller but also the result of the operation or the exception
 * thrown by the operation.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet
 * @param <T>
 *            the type of the result of the operation
 */
public class OperationResultCallbackArguments<C, T> extends
		CallbackArguments<C> {

	private T result;
	private Throwable error;

	/**
	 * Creates the operation callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet controller
	 * @param result
	 *            the result of the operation
	 */
	public OperationResultCallbackArguments(ICloudletController<C> cloudlet,
			T result) {
		super(cloudlet);
		this.result = result;
		this.error = null;
	}

	/**
	 * Creates the operation callback argument.
	 * 
	 * @param cloudlet
	 *            the cloudlet controller
	 * @param error
	 *            the exception thrown by the operation
	 */
	public OperationResultCallbackArguments(ICloudletController<C> cloudlet,
			Throwable error) {
		super(cloudlet);
		this.result = null;
		this.error = error;
	}

	/**
	 * Returns the result of the operation.
	 * 
	 * @return the result of the operation
	 */
	public T getResult() {
		return this.result;
	}

	/**
	 * Returns the exception thrown by the operation if it didn't finish with
	 * success.
	 * 
	 * @return the exception thrown by the operation
	 */
	public Throwable getError() {
		return this.error;
	}

}
