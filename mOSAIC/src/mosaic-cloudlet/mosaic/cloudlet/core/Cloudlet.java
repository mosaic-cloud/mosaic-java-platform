package mosaic.cloudlet.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import mosaic.cloudlet.resources.IResourceAccessor;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.cloudlet.runtime.CloudletExecutor;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.EventDrivenOperation;
import mosaic.core.ops.EventDrivenResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

public class Cloudlet<S extends Object> implements ICloudlet {
	private boolean active;
	private CloudletExecutor executor;
	private CloudletController controller;
	private IConfiguration configuration;
	private ICloudletCallback<S> controllerCallback;
	private S state;

	/**
	 * Creates a new cloudlet instance.
	 * 
	 * @param config
	 *            configuration data required for configuring and initializing
	 *            the cloudlet instance
	 * @throws CloudletException
	 */
	public Cloudlet(S state, ICloudletCallback<S> callback)
			throws CloudletException {
		synchronized (this) {
			this.state = state;
			this.active = false;
			this.executor = new CloudletExecutor();
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
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					Cloudlet.this.controllerCallback.initializeFailed(
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));

				}

			};

			List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
			CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object>(
					complHandler);
			handlers.add(complHandler);
			EventDrivenOperation<Object> initOperation = new EventDrivenOperation<Object>(
					handlers, iHandler);
			initOperation.setOperation(new Runnable() {

				@Override
				public void run() {
					// call the user defined init
					Cloudlet.this.controllerCallback.initialize(
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));
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
				ExceptionTracer.traceRethrown(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceRethrown(e);
			}
		}
		return initialized;
	}

	@Override
	public boolean destroy() {
		boolean destroyed = false;
		synchronized (this) {
			this.active = false;
			// TODO shutdown executor
			IOperationCompletionHandler<Object> complHandler = new IOperationCompletionHandler<Object>() {

				@Override
				public void onSuccess(Object result) {
					Cloudlet.this.controllerCallback.destroySucceeded(
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					Cloudlet.this.controllerCallback.destroyFailed(
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));

				}

			};

			List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
			CompletionInvocationHandler<Object> iHandler = new CloudletResponseInvocationHandler<Object>(
					complHandler);
			handlers.add(complHandler);
			EventDrivenOperation<Object> destroyOperation = new EventDrivenOperation<Object>(
					handlers, iHandler);
			destroyOperation.setOperation(new Runnable() {

				@Override
				public void run() {
					// call the user defined init
					Cloudlet.this.controllerCallback.destroy(
							Cloudlet.this.state, new CallbackArguments<S>(
									controller));
				}
			});
			IResult<Object> result = new EventDrivenResult<Object>(
					destroyOperation);
			this.executor.handleRequest(destroyOperation.getOperation());
			try {
				result.getResult();
				this.executor.shutdown();
				// TODO
				destroyed = true;
			} catch (InterruptedException e) {
				ExceptionTracer.traceRethrown(e);
			} catch (ExecutionException e) {
				ExceptionTracer.traceRethrown(e);
			}
		}
		return destroyed;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	private synchronized void destroyResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler) {
		accessor.destroy(callbackHandler);
	}

	private synchronized void initializeResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler, S cloudletState) {
		accessor.initialize(callbackHandler, this.state);
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
		T proxy = (T) Proxy.newProxyInstance(callback.getClass()
				.getClassLoader(), new Class[] { callback.getClass() },
				iHandler);
		return proxy;
	}

	final class CloudletController implements ICloudletController<S> {
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
		public void initializeResource(IResourceAccessor<S> accessor,
				IResourceAccessorCallback<S> callbackHandler, S cloudletState) {
			Cloudlet.this.initializeResource(accessor, callbackHandler,
					cloudletState);

		}

		@Override
		public void destroyResource(IResourceAccessor<S> accessor,
				IResourceAccessorCallback<S> callbackHandler) {
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
						ExceptionTracer.traceDeferred(e);
					} catch (IllegalAccessException e) {
						ExceptionTracer.traceDeferred(e);
					} catch (InvocationTargetException e) {
						ExceptionTracer.traceDeferred(e);
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
						ExceptionTracer.traceDeferred(e);
					} catch (IllegalAccessException e) {
						ExceptionTracer.traceDeferred(e);
					} catch (InvocationTargetException e) {
						ExceptionTracer.traceDeferred(e);
					}

				}
			};
			Cloudlet.this.executor.handleResponse(task);
			return null;
		}

	}

}
