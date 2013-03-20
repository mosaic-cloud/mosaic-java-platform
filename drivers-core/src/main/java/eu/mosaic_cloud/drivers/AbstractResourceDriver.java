/*
 * #%L
 * mosaic-drivers-core
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

package eu.mosaic_cloud.drivers;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;


/**
 * Base class for the resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractResourceDriver
		implements
			IResourceDriver
{
	/**
	 * Constructs a driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 */
	protected AbstractResourceDriver (final ThreadingContext threading, final int noThreads)
	{
		this.pendingResults = new ArrayList<IResult<?>> ();
		this.executor = threading.createFixedThreadPool (threading.getThreadConfiguration ().override (this, "operations", true), noThreads);
		this.logger = Transcript.create (this, true).adaptAs (Logger.class);
	}
	
	public void addPendingOperation (final IResult<?> pendingOp)
	{
		this.pendingResults.add (pendingOp);
	}
	
	public int countPendingOperations ()
	{
		return this.pendingResults.size ();
	}
	
	@Override
	public synchronized void destroy ()
	{
		IResult<?> pResult;
		this.destroyed = true;
		this.executor.shutdown ();
		// NOTE: cancel all pending operations
		final Iterator<IResult<?>> iter = this.pendingResults.iterator ();
		while (iter.hasNext ()) {
			pResult = iter.next ();
			pResult.cancel ();
			iter.remove ();
		}
	}
	
	/**
	 * Handles unsupported operation errors. The base implementation sends an
	 * error operation to the caller.
	 * 
	 * @param opName
	 *            the name of the operation
	 * @param handler
	 *            the handler used for sending the error
	 */
	public void handleUnsupportedOperationError (final String opName, final IOperationCompletionHandler<?> handler)
	{
		final Runnable task = new Runnable () {
			@Override
			public void run ()
			{
				final Exception error = new UnsupportedOperationException ("Operation " + opName + " is not supported by this driver.");
				handler.onFailure (error);
			}
		};
		this.executeOperation (task);
	}
	
	public void removePendingOperation (final IResult<?> pendingOp)
	{
		this.pendingResults.remove (pendingOp);
	}
	
	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which do not return anything. For the other
	 * operations see {@link AbstractResourceDriver#submitOperation(FutureTask)}
	 * .
	 * 
	 * @param operation
	 *            the operation
	 */
	protected void executeOperation (final Runnable operation)
	{
		this.executor.execute (operation);
	}
	
	protected synchronized boolean isDestroyed ()
	{
		return this.destroyed;
	}
	
	/**
	 * Submit a new asynchronous operation for execution. This operation should
	 * be called for operations which return something. For the other operations
	 * see {@link AbstractResourceDriver#executeOperation(Runnable)}.
	 * 
	 * @param <T>
	 *            the operation's return type
	 * @param operation
	 *            the operation
	 */
	protected <T extends Object> void submitOperation (final FutureTask<T> operation)
	{
		this.executor.submit (operation);
	}
	
	protected final ExecutorService executor;
	protected Logger logger;
	private boolean destroyed = false;
	private final List<IResult<?>> pendingResults;
}
