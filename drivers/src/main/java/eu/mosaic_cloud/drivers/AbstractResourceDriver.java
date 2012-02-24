/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.drivers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Base class for the resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractResourceDriver implements IResourceDriver {

	private final List<IResult<?>> pendingResults;
	private final ExecutorService executor;
	private boolean destroyed = false;
	protected MosaicLogger logger;

	/**
	 * Constructs a driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 */
	protected AbstractResourceDriver(ThreadingContext threading, int noThreads) {
		this.pendingResults = new ArrayList<IResult<?>>();
		this.executor = threading
				.createFixedThreadPool(
						ThreadConfiguration.create(this, "operations", true),
						noThreads);
		this.logger = MosaicLogger.createLogger(this);
	}

	@Override
	public synchronized void destroy() {
		IResult<?> pResult;
		this.destroyed = true;
		this.executor.shutdown();
		// NOTE: cancel all pending operations
		Iterator<IResult<?>> iter = this.pendingResults.iterator();
		while (iter.hasNext()) {
			pResult = iter.next();
			pResult.cancel();
			iter.remove();
		}
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
	protected <T extends Object> void submitOperation(FutureTask<T> operation) {
		this.executor.submit(operation);
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
	protected void executeOperation(Runnable operation) {
		this.executor.execute(operation);
	}

	public int countPendingOperations() {
		return this.pendingResults.size();
	}

	public void removePendingOperation(IResult<?> pendingOp) {
		this.pendingResults.remove(pendingOp);
	}

	public void addPendingOperation(IResult<?> pendingOp) {
		this.pendingResults.add(pendingOp);
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
	public void handleUnsupportedOperationError(final String opName,
			final IOperationCompletionHandler<?> handler) {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Exception error = new UnsupportedOperationException(
						"Operation " + opName
								+ " is not supported by this driver.");
				handler.onFailure(error);
			}
		};
		executeOperation(task);
	}

	protected synchronized boolean isDestroyed() {
			return this.destroyed;
	}

}
