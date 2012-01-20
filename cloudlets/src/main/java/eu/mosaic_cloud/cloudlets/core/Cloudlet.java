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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.CompletionInvocationHandler;
import eu.mosaic_cloud.platform.core.ops.EventDrivenOperation;
import eu.mosaic_cloud.platform.core.ops.EventDrivenResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;

import eu.mosaic_cloud.cloudlets.resources.IResourceAccessor;
import eu.mosaic_cloud.cloudlets.resources.IResourceAccessorCallback;
import eu.mosaic_cloud.cloudlets.runtime.CloudletExecutor;


/**
 * This class handles the internals of cloudlet execution. An object of this
 * class will be created by the container for each user cloudlet. The link
 * between the user cloudlet and this object is done by an
 * {@link ICloudletController} object.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the context of the cloudlet
 */
public class Cloudlet<C extends Object> implements ICloudlet {

	private boolean active;
	private CloudletExecutor executor;
	private CloudletController controller;
	private IConfiguration configuration;
	private ICloudletCallback<C> controllerCallback;
	private C context;

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
	public Cloudlet(C context, ICloudletCallback<C> callback, ClassLoader loader)
			throws CloudletException {
		synchronized (this) {
			this.context = context;
			this.active = false;
			this.executor = new CloudletExecutor(loader);
			this.controller = new CloudletController();

			this.controllerCallback = callback;
			// create the invocation handler for the callback class
			// InvocationHandler handler = new
			// CloudletInvocationHandler(callback);
			// @SuppressWarnings("unchecked")
			// ICloudletCallback<S> proxy = (ICloudletCallback<S>) Proxy
			// .newProxyInstance(callback.getClass().getClassLoader(),
			// new Class[] { callback.getClass() }, handler);
			// this.callbackProxies.put(this.controllerCallback, callback
			// .getClass().cast(proxy));
		}
	}

	@Override
	public boolean initialize(IConfiguration config) {
		boolean initialized = false;
		this.configuration = config;

		synchronized (this) {
			IOperationCompletionHandler<Object> complHandler = new IOperationCompletionHandler<Object>() {

				@Override
				public void onSuccess(Object result) {
					Cloudlet.this.controllerCallback.initializeSucceeded(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					Cloudlet.this.controllerCallback.initializeFailed(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));

				}

			};

			List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
			CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object>(
					complHandler);
			handlers.add(complHandler);
			final EventDrivenOperation<Object> initOperation = new EventDrivenOperation<Object>(
					handlers, iHandler);
			initOperation.setOperation(new Runnable() {

				@Override
				public void run() {
					// call the user defined init
					Cloudlet.this.controllerCallback.initialize(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));
					List<IOperationCompletionHandler<Object>> handlers = initOperation
							.getCompletionHandlers();
					for (IOperationCompletionHandler<Object> handler : handlers) {
						handler.onSuccess(null);
					}
				}
			});
			IResult<Object> result = new EventDrivenResult<Object>(
					initOperation);
			this.executor.handleRequest(initOperation.getOperation());
			try {
				result.getResult();
				initialized = true;
				this.active = true;
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceIgnored(e);
			}
		}
		return initialized;
	}

	@Override
	public boolean destroy() {
		synchronized (this) {
			this.active = false;
			IOperationCompletionHandler<Object> complHandler = new IOperationCompletionHandler<Object>() {

				@Override
				public void onSuccess(Object result) {
					Cloudlet.this.controllerCallback.destroySucceeded(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));
					Cloudlet.this.executor.shutdown();
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					Cloudlet.this.controllerCallback.destroyFailed(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));
					Cloudlet.this.executor.shutdown();
				}

			};

			List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
			CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object>(
					complHandler);
			handlers.add(complHandler);
			final EventDrivenOperation<Object> destroyOperation = new EventDrivenOperation<Object>(
					handlers, iHandler);
			destroyOperation.setOperation(new Runnable() {

				@Override
				public void run() {
					// call the user defined init
					Cloudlet.this.controllerCallback.destroy(
							Cloudlet.this.context, new CallbackArguments<C>(
									Cloudlet.this.controller));
					List<IOperationCompletionHandler<Object>> handlers = destroyOperation
							.getCompletionHandlers();
					for (IOperationCompletionHandler<Object> handler : handlers) {
						handler.onSuccess(null);
					}
				}
			});
			IResult<Object> result = new EventDrivenResult<Object>(
					destroyOperation);
			this.executor.handleRequest(destroyOperation.getOperation());
			try {
				MosaicLogger.getLogger().trace(
						"Cloudlet.destroy() - Waiting for destroy.");
				result.getResult();
				MosaicLogger.getLogger().trace(
						"Cloudlet.destroy() - Cloudlet destroyed.");
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceIgnored(e);
			}
			//			this.executor.shutdown();
			//			// TODO
			//			destroyed = true;
		}
		return true;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	private synchronized void destroyResource(IResourceAccessor<C> accessor,
			IResourceAccessorCallback<C> callbackHandler) {
		accessor.destroy(callbackHandler);
	}

	private synchronized void initializeResource(IResourceAccessor<C> accessor,
			IResourceAccessorCallback<C> callbackHandler, C cloudletState) {
		accessor.initialize(callbackHandler, this.context);
	}

	private <T> CompletionInvocationHandler<T> getResponseHandler(
			IOperationCompletionHandler<T> handler) {
		CloudletResponseInvocationHandler<T> iHandler = new CloudletResponseInvocationHandler<T>(
				handler);
		return iHandler;
	}

	private <T> T getCallbackProxy(Class<T> callbackType, T callback) {
		CloudletInvocationHandler<T> iHandler = new CloudletInvocationHandler<T>(
				callback);
		@SuppressWarnings("unchecked")
		T proxy = (T) Proxy.newProxyInstance(executor.getLoader(),
				new Class[] { callbackType }, iHandler);
		return proxy;
	}

	/**
	 * An implementation of the cloudlet controller. Basically, all operation in
	 * the controller's interface are redirected to operations of the cloudlet
	 * object.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class CloudletController implements ICloudletController<C> {

		@Override
		public final boolean destroy() {
			return (Cloudlet.this.destroy());
		}

		@Override
		public final IConfiguration getConfiguration() {
			return (Cloudlet.this.configuration);
		}

		@Override
		public final boolean isActive() {
			return (Cloudlet.this.isActive());
		}

		@Override
		public boolean initialize(IConfiguration config) {
			throw (new IllegalStateException());
		}

		@Override
		public void initializeResource(IResourceAccessor<C> accessor,
				IResourceAccessorCallback<C> callbackHandler, C cloudletState) {
			Cloudlet.this.initializeResource(accessor, callbackHandler,
					cloudletState);

		}

		@Override
		public void destroyResource(IResourceAccessor<C> accessor,
				IResourceAccessorCallback<C> callbackHandler) {
			Cloudlet.this.destroyResource(accessor, callbackHandler);

		}

		@Override
		public <T> CompletionInvocationHandler<T> getResponseInvocationHandler(
				IOperationCompletionHandler<T> handler) {
			return Cloudlet.this.getResponseHandler(handler);
		}

		@Override
		public <T> T buildCallbackInvoker(T callback, Class<T> callbackType) {
			return Cloudlet.this.getCallbackProxy(callbackType, callback);
		}

	}

	/**
	 * This handler will serialize the execution of requests received by a
	 * cloudlet instance.
	 * 
	 * @author Georgiana Macariu
	 * 
	 * @param <T>
	 *            type to invoke
	 */
	final class CloudletInvocationHandler<T> implements InvocationHandler {

		private T callback;

		/**
		 * Creates the handler
		 * 
		 * @param callback
		 *            the callback to execute
		 */
		public CloudletInvocationHandler(T callback) {
			super();
			this.callback = callback;
		}

		@Override
		public Object invoke(Object proxy, final Method method,
				final Object[] arguments) throws Throwable {
			Runnable task = new Runnable() {

				@Override
				public void run() {
					try {
						method.invoke(CloudletInvocationHandler.this.callback,
								arguments);
					} catch (IllegalArgumentException e) {
						ExceptionTracer.traceIgnored(e);
					} catch (IllegalAccessException e) {
						ExceptionTracer.traceIgnored(e);
					} catch (InvocationTargetException e) {
						ExceptionTracer.traceIgnored(e);
					}

				}
			};
			Cloudlet.this.executor.handleRequest(task);
			return null;
		}

	}

	/**
	 * This handler will serialize the execution of response handlers received
	 * by a cloudlet instance.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class CloudletResponseInvocationHandler<T> extends
			CompletionInvocationHandler<T> {

		/**
		 * Creates the handler.
		 * 
		 * @param handler
		 *            the response handler to execute
		 */
		private CloudletResponseInvocationHandler(
				IOperationCompletionHandler<T> handler) {
			super(handler);

		}

		@Override
		public CompletionInvocationHandler<T> createHandler(
				IOperationCompletionHandler<T> handler) {
			return new CloudletResponseInvocationHandler<T>(handler);
		}

		@Override
		public Object invoke(Object proxy, final Method method,
				final Object[] arguments) throws Throwable {
			Runnable task = new Runnable() {

				@Override
				public void run() {
					try {
						method.invoke(
								CloudletResponseInvocationHandler.this.handler,
								arguments);
					} catch (IllegalArgumentException e) {
						ExceptionTracer.traceIgnored(e);
					} catch (IllegalAccessException e) {
						ExceptionTracer.traceIgnored(e);
					} catch (InvocationTargetException e) {
						ExceptionTracer.traceIgnored(e);
					}

				}
			};
			Cloudlet.this.executor.handleResponse(task);
			return null;
		}

	}

}
