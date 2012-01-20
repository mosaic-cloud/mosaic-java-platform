/*
 * #%L
 * mosaic-platform-core
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
package eu.mosaic_cloud.platform.core.ops;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Interface for handling the result of any asynchronous operation. An object of
 * this type acts like a handle for the actual result of the operation. To
 * implement this interface, you can write a generic class extending
 * {@link GenericResult} or {@link EventDrivenResult}.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IResult<T> {

	/**
	 * Tests if the operation which will produce this result is completed and
	 * its result is ready for further processing.
	 * 
	 * @return <code>true</code> if the result is available for further
	 *         processing.
	 */
	boolean isDone();

	/**
	 * Tests if the operation which will produce this result was cancelled
	 * before it completed normally.
	 * 
	 * @return <code>true</code> if the operation was cancelled before it
	 *         completed
	 */
	boolean cancel();

	/**
	 * Waits if necessary for the asynchronous operation to complete, and then
	 * retrieves its result.
	 * 
	 * @return the computed result
	 * @throws InterruptedException
	 *             if the current thread running the operation was interrupted
	 *             while waiting
	 * @throws ExecutionException
	 *             if the operation threw an exception
	 */
	T getResult() throws InterruptedException, ExecutionException;

	T getResult(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException;
}
