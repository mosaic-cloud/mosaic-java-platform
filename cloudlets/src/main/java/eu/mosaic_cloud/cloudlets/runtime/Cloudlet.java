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

package eu.mosaic_cloud.cloudlets.runtime;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletException;
import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.CompletionInvocationHandler;
import eu.mosaic_cloud.platform.core.ops.EventDrivenOperation;
import eu.mosaic_cloud.platform.core.ops.EventDrivenResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * This class handles the internals of cloudlet execution. An object of this
 * class will be created by the container for each user cloudlet. The link
 * between the user cloudlet and this object is done by an
 * {@link ICloudletController} object.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Context>
 *            the context of the cloudlet
 */
public class Cloudlet<Context extends Object>
{
	/**
	 * Creates a new cloudlet instance.
	 * 
	 * @param config
	 *            configuration data required for configuring and initializing
	 *            the cloudlet instance
	 * @param loader
	 *            the class loader used for loading cloudlet classes
	 * @throws CloudletException
	 */
	public Cloudlet (final Context context, final ICloudletCallback<Context> callback, final IConfiguration configuration, final ThreadingContext threading, final ClassLoader classLoader)
	{
		super ();
		this.monitor = Monitor.create (this);
		synchronized (this.monitor) {
			this.cloudletContext = context;
			this.cloudletCallback = callback;
			this.configuration = configuration;
			this.threading = threading;
			this.classLoader = classLoader;
			this.cloudletController = new CloudletController ();
			this.executor = new CloudletExecutor (this.threading, this.classLoader);
			this.active = false;
		}
	}
	
	public boolean destroy ()
	{
		synchronized (this.monitor) {
			final IOperationCompletionHandler<Object> complHandler = new IOperationCompletionHandler<Object> () {
				@Override
				public void onFailure (final Throwable error)
				{
					Cloudlet.this.cloudletCallback.destroyFailed (Cloudlet.this.cloudletContext, new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.cloudletController));
					Cloudlet.this.executor.shutdown ();
				}
				
				@Override
				public void onSuccess (final Object result)
				{
					Cloudlet.this.cloudletCallback.destroySucceeded (Cloudlet.this.cloudletContext, new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.cloudletController));
					Cloudlet.this.executor.shutdown ();
				}
			};
			final List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>> ();
			final CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object> (complHandler);
			handlers.add (complHandler);
			final EventDrivenOperation<Object> destroyOperation = new EventDrivenOperation<Object> (handlers, iHandler);
			destroyOperation.setOperation (new Runnable () {
				@Override
				public void run ()
				{
					Cloudlet.this.cloudletCallback.destroy (Cloudlet.this.cloudletContext, new CloudletCallbackArguments<Context> (Cloudlet.this.cloudletController));
					final List<IOperationCompletionHandler<Object>> handlers = destroyOperation.getCompletionHandlers ();
					for (final IOperationCompletionHandler<Object> handler : handlers) {
						handler.onSuccess (null);
					}
				}
			});
			final IResult<Object> result = new EventDrivenResult<Object> (destroyOperation);
			this.executor.handleRequest (destroyOperation.getOperation ());
			try {
				Cloudlet.logger.trace ("Cloudlet.destroy() - Waiting for destroy.");
				result.getResult ();
				Cloudlet.logger.trace ("Cloudlet.destroy() - Cloudlet destroyed.");
			} catch (final InterruptedException e) {
				ExceptionTracer.traceIgnored (e);
			} catch (final ExecutionException e) {
				ExceptionTracer.traceIgnored (e);
			}
			this.active = false;
			return true;
		}
	}
	
	public boolean initialize ()
	{
		synchronized (this.monitor) {
			boolean initialized = false;
			final IOperationCompletionHandler<Object> complHandler = new IOperationCompletionHandler<Object> () {
				@Override
				public void onFailure (final Throwable error)
				{
					Cloudlet.this.cloudletCallback.initializeFailed (Cloudlet.this.cloudletContext, new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.cloudletController));
				}
				
				@Override
				public void onSuccess (final Object result)
				{
					Cloudlet.this.cloudletCallback.initializeSucceeded (Cloudlet.this.cloudletContext, new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.cloudletController));
				}
			};
			final List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>> ();
			final CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object> (complHandler);
			handlers.add (complHandler);
			final EventDrivenOperation<Object> initOperation = new EventDrivenOperation<Object> (handlers, iHandler);
			initOperation.setOperation (new Runnable () {
				@Override
				public void run ()
				{
					Cloudlet.this.cloudletCallback.initialize (Cloudlet.this.cloudletContext, new CloudletCallbackArguments<Context> (Cloudlet.this.cloudletController));
					final List<IOperationCompletionHandler<Object>> handlers = initOperation.getCompletionHandlers ();
					for (final IOperationCompletionHandler<Object> handler : handlers) {
						handler.onSuccess (null);
					}
				}
			});
			final IResult<Object> result = new EventDrivenResult<Object> (initOperation);
			this.executor.handleRequest (initOperation.getOperation ());
			try {
				result.getResult ();
				initialized = true;
				this.active = true;
			} catch (final InterruptedException e) {
				ExceptionTracer.traceIgnored (e);
			} catch (final ExecutionException e) {
				ExceptionTracer.traceIgnored (e);
			}
			return initialized;
		}
	}
	
	public boolean isActive ()
	{
		return this.active;
	}
	
	volatile boolean active;
	final ClassLoader classLoader;
	final ICloudletCallback<Context> cloudletCallback;
	final Context cloudletContext;
	final CloudletController cloudletController;
	final IConfiguration configuration;
	final CloudletExecutor executor;
	final Monitor monitor;
	final ThreadingContext threading;
	static final MosaicLogger logger = MosaicLogger.createLogger (Cloudlet.class);
	
	/**
	 * An implementation of the cloudlet controller. Basically, all operation in
	 * the controller's interface are redirected to operations of the cloudlet
	 * object.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	class CloudletController
			implements
				ICloudletController<Context>
	{
		@Override
		public boolean destroy ()
		{
			return (Cloudlet.this.destroy ());
		}
		
		@Override
		public IConfiguration getConfiguration ()
		{
			return (Cloudlet.this.configuration);
		}
		
		@Override
		public <Factory extends IConnectorFactory<?>> Factory getConnectorFactory (final Class<Factory> factory)
		{
			// FIXME
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public boolean isActive ()
		{
			return (Cloudlet.this.isActive ());
		}
	}
	
	/**
	 * This handler will serialize the execution of response handlers received
	 * by a cloudlet instance.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	class CloudletResponseInvocationHandler<T>
			extends CompletionInvocationHandler<T>
	{
		/**
		 * Creates the handler.
		 * 
		 * @param handler
		 *            the response handler to execute
		 */
		CloudletResponseInvocationHandler (final IOperationCompletionHandler<T> handler)
		{
			super (handler);
		}
		
		@Override
		public CompletionInvocationHandler<T> createHandler (final IOperationCompletionHandler<T> handler)
		{
			return new CloudletResponseInvocationHandler<T> (handler);
		}
		
		@Override
		public Object invoke (final Object proxy, final Method method, final Object[] arguments)
		{
			final Runnable task = new Runnable () {
				@Override
				public void run ()
				{
					try {
						method.invoke (CloudletResponseInvocationHandler.this.handler, arguments);
					} catch (final IllegalArgumentException e) {
						ExceptionTracer.traceIgnored (e);
					} catch (final IllegalAccessException e) {
						ExceptionTracer.traceIgnored (e);
					} catch (final InvocationTargetException e) {
						ExceptionTracer.traceIgnored (e);
					}
				}
			};
			Cloudlet.this.executor.handleResponse (task);
			return null;
		}
	}
}
