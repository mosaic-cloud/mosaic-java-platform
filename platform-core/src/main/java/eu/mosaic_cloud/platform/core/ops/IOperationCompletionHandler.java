/*
 * #%L
 * mosaic-platform-core
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.platform.core.ops;


/**
 * Interface for handlers to be called when the result of an event-drive
 * asynchronous operation arrives.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            the type of the result of the operation
 */
public interface IOperationCompletionHandler<T>
{
	/**
	 * Handles the erroneous finish of an operation.
	 * 
	 * @param <E>
	 *            the type of the error
	 * @param error
	 *            the error
	 */
	void onFailure (Throwable error);
	
	/**
	 * Handles the result of the operation. This shall be called when operation
	 * finishes successfully.
	 * 
	 * @param result
	 *            the result
	 */
	void onSuccess (T result);
}
