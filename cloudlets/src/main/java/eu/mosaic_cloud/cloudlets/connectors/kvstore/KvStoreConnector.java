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
package eu.mosaic_cloud.cloudlets.connectors.kvstore;

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.core.ConnectorException;
import eu.mosaic_cloud.cloudlets.connectors.core.ConnectorStatus;
import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.core.OperationResultCallbackArguments;
import eu.mosaic_cloud.cloudlets.runtime.CloudletComponentCallbacks.ResourceType;
import eu.mosaic_cloud.cloudlets.runtime.CloudletComponentResourceFinder;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.Miscellaneous;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Base cloudlet-level accessor for key value storages. Cloudlets will use an
 * object of this type to get access to a key-value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using the accessor
 */
public class KvStoreConnector<C> implements IKvStoreConnector<C> {

	private IConfiguration configuration;
	protected ICloudletController<C> cloudlet;
	protected C cloudletContext;
	protected DataEncoder<?> dataEncoder;
	private ConnectorStatus status;
	private eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector<?> connector;
	private IKvStoreConnectorCallback<C> callback;
	private IKvStoreConnectorCallback<C> callbackProxy;
	private Monitor monitor = Monitor.create (this);

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
	public KvStoreConnector(IConfiguration config,
			ICloudletController<C> cloudlet, DataEncoder<?> encoder) {
		synchronized (this.monitor) {
			this.configuration = config;
			this.cloudlet = cloudlet;
			this.dataEncoder = encoder;
			this.status = ConnectorStatus.CREATED;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(IConnectorCallback<C> callback, C context,
			ThreadingContext threading) {
		synchronized (this.monitor) {
			this.status = ConnectorStatus.INITIALIZING;
			this.cloudletContext = context;
			this.callback = Miscellaneous.cast(IKvStoreConnectorCallback.class,
					callback);

			this.callbackProxy = this.cloudlet.buildCallbackInvoker(
					this.callback, IKvStoreConnectorCallback.class);
			try {
				String connectorName = ConfigUtils.resolveParameter(
						this.configuration,
						eu.mosaic_cloud.cloudlets.tools.ConfigProperties
								.getString("KvStoreConnector.0"), String.class, //$NON-NLS-1$
						""); //$NON-NLS-1$
				ResourceType type = ResourceType.KEY_VALUE;
				if (connectorName
						.equalsIgnoreCase(KeyValueConnectorFactory.ConnectorType.MEMCACHED
								.toString())) {
					type = ResourceType.MEMCACHED;
				}

				if (!CloudletComponentResourceFinder.getResourceFinder().findResource(type,
						this.configuration)) {
					throw new ConnectorException(
							"Cannot find a resource of type " + type.toString());
				}
				this.connector = KeyValueConnectorFactory.createConnector(
						connectorName, this.configuration, this.dataEncoder,
						threading);
				this.status = ConnectorStatus.READY;
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						KvStoreConnector.this.cloudlet, true);
				this.callbackProxy.initializeSucceeded(this.cloudletContext,
						arguments);
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						this.cloudlet, e);
				this.callbackProxy.initializeFailed(context, arguments);
			}
		}
	}

	@Override
	public void destroy(IConnectorCallback<C> callback) {
		synchronized (this.monitor) {
			this.status = ConnectorStatus.DESTROYING;
			try {
				this.connector.destroy();
				this.status = ConnectorStatus.DESTROYED;
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						KvStoreConnector.this.cloudlet, true);
				this.callbackProxy.destroySucceeded(this.cloudletContext,
						arguments);
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						this.cloudlet, e);
				this.callbackProxy.destroyFailed(this.cloudletContext,
						arguments);
				ExceptionTracer.traceDeferred(e);
			}
		}
	}

	@Override
	public ConnectorStatus getStatus() {
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
	protected <T extends eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector<?>> T getConnector(Class<T> connectorClass) {
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
	protected <T extends IKvStoreConnectorCallback<C>> T getCallback(
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
	protected <T extends IKvStoreConnectorCallback<C>> T getCallbackProxy(
			Class<T> callbackClass) {
		return callbackClass.cast(this.callbackProxy);
	}

	@Override
	public IResult<Boolean> set(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, value, extra);
				KvStoreConnector.this.callback.setSucceeded(
						KvStoreConnector.this.cloudletContext, arguments);

			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, error, extra);
				KvStoreConnector.this.callback.setFailed(
						KvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return this.connector.set(key, value, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Object> get(final String key, final Object extra) {
		IOperationCompletionHandler<Object> cHandler = new IOperationCompletionHandler<Object>() {

			@Override
			public void onSuccess(Object result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, result, extra);
				KvStoreConnector.this.callback.getSucceeded(
						KvStoreConnector.this.cloudletContext, arguments);

			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, error, extra);
				KvStoreConnector.this.callback.getFailed(
						KvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Object>> handlers = new ArrayList<IOperationCompletionHandler<Object>>();
		handlers.add(cHandler);
		return this.connector.get(key, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> delete(final String key, final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, null, extra);
				KvStoreConnector.this.callback.deleteSucceeded(
						KvStoreConnector.this.cloudletContext, arguments);

			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, key, error, extra);
				KvStoreConnector.this.callback.deleteFailed(
						KvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return this.connector.delete(key, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<List<String>> list(final Object extra) {
		IOperationCompletionHandler<List<String>> cHandler = new IOperationCompletionHandler<List<String>>() {

			@Override
			public void onSuccess(List<String> result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, result, null, extra);
				KvStoreConnector.this.callback.deleteSucceeded(
						KvStoreConnector.this.cloudletContext, arguments);

			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						KvStoreConnector.this.cloudlet, "", error, extra); //$NON-NLS-1$
				KvStoreConnector.this.callback.deleteFailed(
						KvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlers.add(cHandler);
		return this.connector.list(handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

}
