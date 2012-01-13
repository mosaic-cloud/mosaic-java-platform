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

import eu.mosaic_cloud.cloudlet.resources.IResourceAccessor;
import eu.mosaic_cloud.cloudlet.resources.IResourceAccessorCallback;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.ops.CompletionInvocationHandler;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;

/**
 * Interface for cloudlet control operations. Each cloudlet has access to an
 * object implementing this interface and uses it to ask for resources or
 * destroying them when they are not required anymore.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 */
public interface ICloudletController<S> extends ICloudlet {
	IConfiguration getConfiguration();

	<T> CompletionInvocationHandler<T> getResponseInvocationHandler(
			IOperationCompletionHandler<T> handler);

	<T> T buildCallbackInvoker(T callback, Class<T> callbackType);

	/**
	 * Initializes the resource accessor for a given resource.
	 * 
	 * @param accessor
	 *            the resource accessor
	 * @param callbackHandler
	 *            the cloudlet callback handler which must handle callbacks to
	 *            operations invoked on the accessor
	 * @param cloudletState
	 *            the cloudlet state
	 */
	void initializeResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler, S cloudletState);

	/**
	 * Destroys the resource accessor for a given resource.
	 * 
	 * @param accessor
	 *            the resource accessor
	 * @param callbackHandler
	 *            the cloudlet callback handler which must handle callbacks to
	 *            operations invoked on the accessor
	 */
	void destroyResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler);

}
