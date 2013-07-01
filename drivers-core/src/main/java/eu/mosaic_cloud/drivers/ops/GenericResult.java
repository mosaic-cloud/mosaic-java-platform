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

package eu.mosaic_cloud.drivers.ops;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Defines a generic result handle of asynchronous operation. The asynchronous
 * operation is implemented using Java Futures in this case. It implements the
 * {@link IResult} interface.
 * <p>
 * If you would like to use a GenericResult for the sake of managing
 * asynchronous operation but not provide a usable result, you can declare types
 * of the form GenericResult<?> and return null as a result of the underlying
 * operation.
 * <p>
 * For working with {@link GenericOperation} types, you should either use this
 * class or extend it instead of implementing another one directly from
 * {@link IResult}.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the asynchronous operation.
 */
public class GenericResult<T>
		implements
			IResult<T>
{
	public GenericResult (final GenericOperation<T> operation)
	{
		this.operation = operation;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.mosaic_cloud.drivers.ops.IResult#cancel()
	 */
	@Override
	public final boolean cancel ()
	{
		boolean done;
		// NOTE: first test if it was not already cancelled
		done = this.operation.isCancelled ();
		if (!done) {
			// NOTE: try to cancel the operation
			done = this.operation.cancel ();
			// NOTE: cancellation may have failed if the operation was already
			//-- finished
			if (!done) {
				done = this.operation.isDone ();
			}
		}
		return done;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.mosaic_cloud.platform.v1.core.IResult#getResult()
	 */
	@Override
	public T getResult ()
			throws InterruptedException,
				ExecutionException
	{
		return this.operation.get ();
	}
	
	@Override
	public T getResult (final long timeout, final TimeUnit unit)
			throws InterruptedException,
				ExecutionException,
				TimeoutException
	{
		return this.operation.get (timeout, unit);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.mosaic_cloud.drivers.ops.IResult#isDone()
	 */
	@Override
	public final boolean isDone ()
	{
		return this.operation.isDone ();
	}
	
	private final GenericOperation<T> operation;
}
