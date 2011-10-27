/*
 * #%L
 * mosaic-cloudlet
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.cloudlet.resources.kvstore;

import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ContainerException;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.cloudlet.resources.ResourceStatus;
import mosaic.cloudlet.runtime.ContainerComponentCallbacks.ResourceType;
import mosaic.cloudlet.runtime.ResourceFinder;
import mosaic.connector.kvstore.IKeyValueStore;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.DataEncoder;
import mosaic.core.utils.Miscellaneous;

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
	protected DataEncoder<?> dataEncoder;
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
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public KeyValueAccessor(IConfiguration config,
			ICloudletController<S> cloudlet, DataEncoder<?> encoder) {
		this.configuration = config;
		this.cloudlet = cloudlet;
		this.dataEncoder = encoder;
		this.status = ResourceStatus.CREATED;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		synchronized (this) {
			this.status = ResourceStatus.INITIALIZING;
			this.cloudletState = state;
			this.callback = Miscellaneous.cast(IKeyValueAccessorCallback.class,
					callback);

			this.callbackProxy = this.cloudlet.buildCallbackInvoker(
					this.callback, IKeyValueAccessorCallback.class);
			try {
				String connectorName = ConfigUtils.resolveParameter(
						this.configuration, mosaic.cloudlet.ConfigProperties
								.getString("KeyValueAccessor.0"), String.class, //$NON-NLS-1$
						""); //$NON-NLS-1$
				ResourceType type = ResourceType.KEY_VALUE;
				if (connectorName
						.equalsIgnoreCase(KeyValueConnectorFactory.ConnectorType.MEMCACHED
								.toString())) {
					type = ResourceType.MEMCACHED;
				}

				if (!ResourceFinder.getResourceFinder().findResource(type,
						this.configuration))
					throw new ContainerException(
							"Cannot find a resource of type " + type.toString());
				this.connector = KeyValueConnectorFactory.createConnector(
						connectorName, this.configuration, this.dataEncoder);// MemcachedStoreConnector.create(configuration);
				this.status = ResourceStatus.READY;
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						KeyValueAccessor.this.cloudlet, true);
				this.callbackProxy.initializeSucceeded(this.cloudletState,
						arguments);
			} catch (Throwable e) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						this.cloudlet, e);
				this.callbackProxy.initializeFailed(state, arguments);
				ExceptionTracer.traceDeferred(e);
			}
		}
	}

	@Override
	public void destroy(IResourceAccessorCallback<S> callback) {
		synchronized (this) {
			this.status = ResourceStatus.DESTROYING;
			try {
				this.connector.destroy();
				this.status = ResourceStatus.DESTROYED;
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						KeyValueAccessor.this.cloudlet, true);
				this.callbackProxy.destroySucceeded(this.cloudletState,
						arguments);
			} catch (Throwable e) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						this.cloudlet, e);
				this.callbackProxy.destroyFailed(this.cloudletState, arguments);
				ExceptionTracer.traceDeferred(e);
			}
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
	public IResult<Boolean> set(final String key, final Object value,
			final Object extra) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, value, extra);
					KeyValueAccessor.this.callback.setSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error, extra);
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
	public IResult<Object> get(final String key, final Object extra) {
		synchronized (this) {
			IOperationCompletionHandler<Object> cHandler = new IOperationCompletionHandler<Object>() {

				@Override
				public void onSuccess(Object result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, result, extra);
					KeyValueAccessor.this.callback.getSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error, extra);
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
	public IResult<Boolean> delete(final String key, final Object extra) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, null, extra);
					KeyValueAccessor.this.callback.deleteSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, key, error, extra);
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

	@Override
	public IResult<List<String>> list(final Object extra) {
		synchronized (this) {
			IOperationCompletionHandler<List<String>> cHandler = new IOperationCompletionHandler<List<String>>() {

				@Override
				public void onSuccess(List<String> result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, result, null, extra);
					KeyValueAccessor.this.callback.deleteSucceeded(
							KeyValueAccessor.this.cloudletState, arguments);

				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							KeyValueAccessor.this.cloudlet, "", error, extra); //$NON-NLS-1$
					KeyValueAccessor.this.callback.deleteFailed(
							KeyValueAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
			handlers.add(cHandler);
			return this.connector.list(handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

}
