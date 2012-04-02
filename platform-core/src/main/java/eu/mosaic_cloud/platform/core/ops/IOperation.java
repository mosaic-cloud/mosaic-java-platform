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
 * Interface for asynchronous operations.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the operation.
 */
public interface IOperation<T> {

    /**
     * Cancels the asynchronous operation.
     * 
     * @return <code>true</code> if operation was cancelled
     */
    boolean cancel();

    /**
     * Waits if necessary for the computation to complete, and then retrieves
     * its result.
     * 
     * @return the computed result
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if the computation threw an exception
     */
    T get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation to
     * complete, and then retrieves its result, if available.
     * 
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the time unit of the timeout argument
     * @return the computed result
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if the computation threw an exception
     * @throws TimeoutException
     *             if the wait timed out
     */
    T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException;

    /**
     * Returns <code>true</code> if this task was cancelled before it completed
     * normally.
     * 
     * @return <code>true</code> if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns <code>true</code> if this task completed. Completion may be due
     * to normal termination, an exception, or cancellation -- in all of these
     * cases, this method will return <code>true</code>.
     * 
     * @return <code>true</code> if this task completed
     */
    boolean isDone();
}
