package mosaic.cloudlet.resources.kvstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.cloudlet.resources.ResourceStatus;
import mosaic.connector.kvstore.IKeyValueStore;
import mosaic.connector.kvstore.MemcachedStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Base cloudlet-level accessor for key value storages. Cloudlets will use an
 * object of this type to get access to a key-value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using the accessor
 */
public class KeyValueAccessor<S> implements IKeyValueAccessor<S> {

	private IConfiguration configuration;
	protected ICloudletController<S> cloudlet;
	protected S cloudletState;
	private ResourceStatus status;
	private IKeyValueStore connector;
	private IKeyValueAccessorCallback<S> callback;
	private IKeyValueAccessorCallback<S> callbackProxy;

	/**
	 * Creates a new accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 */
	public KeyValueAccessor(IConfiguration config,
			ICloudletController<S> cloudlet) {
		this.configuration = config;
		this.cloudlet = cloudlet;
		this.status = ResourceStatus.CREATED;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		synchronized (this) {
			this.status = ResourceStatus.INITIALIZING;
			this.cloudletState = state;

			this.callbackProxy = this.cloudlet.buildCallbackInvoker(
					this.callback, IKeyValueAccessorCallback.class);
			try {
				// FIXME select the correct connector
				this.connector = MemcachedStoreConnector.create(configuration);
				this.status = ResourceStatus.READY;
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						KeyValueAccessor.this.cloudlet, true);
				this.callbackProxy
						.initializeSucceeded(cloudletState, arguments);
			} catch (IOException e) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						this.cloudlet, e);
				this.callbackProxy.initializeFailed(state, arguments);
				ExceptionTracer.traceRethrown(e);
			}
		}
	}

	@Override
	public void destroy(IResourceAccessorCallback<S> callback) {
		synchronized (this) {
			this.status = ResourceStatus.DESTROYING;
			this.connector.destroy();
			this.status = ResourceStatus.DESTROYED;
			CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
					KeyValueAccessor.this.cloudlet, true);
			this.callbackProxy.destroySucceeded(cloudletState, arguments);
		}
	}

	@Override
	public ResourceStatus getStatus() {
		return this.status;
	}

	/**
	 * Returns the key value storage connector used by this accessor.
	 * 
	 * @param <T>
	 *            the type of the connector
	 * @param connectorClass
	 *            the class of the connector
	 * @return the connector
	 */
	protected <T extends IKeyValueStore> T getConnector(Class<T> connectorClass) {
		return connectorClass.cast(this.connector);
	}

	/**
	 * Returns the callback for this accessor.
	 * 
	 * @param <T>
	 *            the type of the callback
	 * @param callbackClass
	 *            the class of the callback
	 * @return the callback
	 */
	protected <T extends IKeyValueAccessorCallback<S>> T getCallback(
			Class<T> callbackClass) {
		return callbackClass.cast(this.callback);
	}

	/**
	 * Returns a dynamic proxy of the callback which makes sure that the
	 * callback will be executed within the thread allocated to the cloudlet
	 * instance.
	 * 
	 * @param <T>
	 *            the type of the callback
	 * @param callbackClass
	 *            the class of the callback
	 * @return a dynamic proxy of the callback
	 */
	protected <T extends IKeyValueAccessorCallback<S>> T getCallbackProxy(
			Class<T> callbackClass) {
		return callbackClass.cast(this.callbackProxy);
	}

	@Override
	public IResult<Boolean> set(final String key, final Object value) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, value);
					KeyValueAccessor.this.callback.setSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error);
					KeyValueAccessor.this.callback.setFailed(
							KeyValueAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return this.connector.set(key, value, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Object> get(final String key) {
		synchronized (this) {
			IOperationCompletionHandler<Object> cHandler = new IOperationCompletionHandler<Object>() {

				@Override
				public void onSuccess(Object result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, result);
					KeyValueAccessor.this.callback.getSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error);
					KeyValueAccessor.this.callback.getFailed(
							KeyValueAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
			handlers.add(cHandler);
			return this.connector.get(key, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> delete(final String key) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, null);
					KeyValueAccessor.this.callback.deleteSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error);
					KeyValueAccessor.this.callback.deleteFailed(
							KeyValueAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return this.connector.delete(key, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

}
