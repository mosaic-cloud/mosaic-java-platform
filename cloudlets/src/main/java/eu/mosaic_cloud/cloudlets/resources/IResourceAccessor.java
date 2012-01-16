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
package eu.mosaic_cloud.cloudlets.resources;

/**
 * Interface for all resource accessors used by cloudlets.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the cloudlet context
 */
public interface IResourceAccessor<C> {
	/**
	 * Initialize the accessor.
	 * 
	 * @param callback
	 *            handler for callbacks received from the resource
	 * @param context
	 *            cloudlet context
	 */
	void initialize(IResourceAccessorCallback<C> callback, C context);

	/**
	 * Destroys the accessor.
	 * 
	 * @param callback
	 *            handler for callbacks received from the resource
	 */
	void destroy(IResourceAccessorCallback<C> callback);

	/**
	 * Returns the current status of the accessor.
	 * 
	 * @return the current status of the accessor
	 */
	ResourceStatus getStatus();
}
