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
package eu.mosaic_cloud.cloudlets.resources.amqp;

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlets.ConfigProperties;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ContainerException;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.core.OperationResultCallbackArguments;
import eu.mosaic_cloud.cloudlets.resources.IResourceAccessorCallback;
import eu.mosaic_cloud.cloudlets.resources.ResourceStatus;
import eu.mosaic_cloud.cloudlets.runtime.ContainerComponentCallbacks.ResourceType;
import eu.mosaic_cloud.cloudlets.runtime.ResourceFinder;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConnector;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConnector;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Base accessor class for AMQP queuing systems.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using the accessor
 * @param <D>
 *            the type of messages processed by the accessor
 */
public abstract class AmqpQueueAccessor<C, D extends Object> implements
		IAmqpQueueAccessor<C> {

	private IConfiguration configuration;
	protected ICloudletController<C> cloudlet;
	protected C cloudletContext;
	private ResourceStatus status;
	private IAmqpQueueConnector connector;
	protected Class<D> dataClass;
	protected DataEncoder<D> dataEncoder;

	protected String exchange;
	protected String routingKey;
	protected String queue;
	protected boolean exclusive = true;
	protected boolean autoDelete = true;
	protected boolean passive = false;
	protected boolean durable = false;
	protected AmqpExchangeType exchangeType = AmqpExchangeType.DIRECT;
	protected boolean registered;
	protected MosaicLogger logger;
	protected Monitor monitor = Monitor.create (this);

	/**
	 * Creates a new AMQP resource accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor and connector
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param dataClass
	 *            the type of the consumed or produced messages
	 * @param consumer
	 *            whether to create a consumer or a producer
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public AmqpQueueAccessor(IConfiguration config,
			ICloudletController<C> cloudlet, Class<D> dataClass,
			boolean consumer, DataEncoder<D> encoder) {
		synchronized (this.monitor) {
			this.configuration = config;
			this.logger=MosaicLogger.createLogger(this);
			this.cloudlet = cloudlet;
			this.status = ResourceStatus.CREATED;
			this.dataClass = dataClass;
			this.dataEncoder = encoder;
			this.registered = false;
			String specification = ConfigProperties
					.getString("AmqpQueueAccessor.3"); //$NON-NLS-1$
			if (consumer) {
				specification = ConfigProperties.getString("AmqpQueueAccessor.4"); //$NON-NLS-1$
			}
			IConfiguration accessorConfig = config
					.spliceConfiguration(ConfigurationIdentifier
							.resolveRelative(specification));
			this.configuration = accessorConfig;
			this.exchange = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.0"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.routingKey = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.queue = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.2"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			String type = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.5"), String.class, "").toUpperCase();//$NON-NLS-1$ //$NON-NLS-2$
			if (!type.equals("") && (AmqpExchangeType.valueOf(type) != null)) {
				this.exchangeType = AmqpExchangeType.valueOf(type);
			}
			this.exclusive = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.6"), Boolean.class, true); //$NON-NLS-1$ 
			this.autoDelete = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.7"), Boolean.class, true); //$NON-NLS-1$
			this.passive = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.8"), Boolean.class, false); //$NON-NLS-1$ 
			this.durable = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueAccessor.9"), Boolean.class, false); //$NON-NLS-1$ 
			this.logger.trace(
					"Queue accessor for exchange '" + this.exchange
							+ "', routing key '" + this.routingKey
							+ "' and queue '" + this.queue + "'");
		}
	}

	@Override
	public void initialize(IResourceAccessorCallback<C> callback, C context,
			ThreadingContext threading) {
		synchronized (this.monitor) {
			@SuppressWarnings("unchecked")
			IResourceAccessorCallback<C> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IResourceAccessorCallback.class);
			try {
				this.status = ResourceStatus.INITIALIZING;
				this.cloudletContext = context;
				if (!ResourceFinder.getResourceFinder().findResource(
						ResourceType.AMQP, this.configuration)) {
					throw new ContainerException(
							"Cannot find a resource of type "
									+ ResourceType.AMQP.toString());
				}
				this.connector = AmqpQueueConnector.create(this.configuration,
						threading);
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						AmqpQueueAccessor.this.cloudlet, true);
				proxy.initializeSucceeded(AmqpQueueAccessor.this.cloudletContext,
						arguments);
				this.status = ResourceStatus.INITIALIZED;
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						AmqpQueueAccessor.this.cloudlet, e);
				proxy.initializeFailed(context, arguments);
			}
		}
	}

	@Override
	public void destroy(IResourceAccessorCallback<C> callback) {
		synchronized (this.monitor) {
			this.status = ResourceStatus.DESTROYING;
			@SuppressWarnings("unchecked")
			IResourceAccessorCallback<C> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IResourceAccessorCallback.class);
			try {
				this.logger.trace(
						"AmqpQueueAccessor is destroying the connector...");
				this.connector.destroy();
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						AmqpQueueAccessor.this.cloudlet, true);
				proxy.destroySucceeded(this.cloudletContext, arguments);
				this.logger.trace(
						"AmqpQueueAccessor destroyed successfully.");
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
						AmqpQueueAccessor.this.cloudlet, e);
				proxy.destroyFailed(this.cloudletContext, arguments);
			}
			this.status = ResourceStatus.DESTROYED;
		}
	}

	/**
	 * Returns the resource connector.
	 * 
	 * @return the resource connector
	 */
	protected IAmqpQueueConnector getConnector() {
		return this.connector;
	}

	@Override
	public ResourceStatus getStatus() {
		return this.status;
	}

	protected void declareExchange(final IAmqpQueueAccessorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.declareQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareExchange(this.exchange, this.exchangeType,
				this.durable, this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void declareQueue(final IAmqpQueueAccessorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.bindQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareQueue(this.queue, this.exclusive, this.durable,
				this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void bindQueue(final IAmqpQueueAccessorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.finishRegister(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector()
				.bindQueue(this.exchange, this.queue, this.routingKey,
						cHandlers,
						this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	protected void startRegister(IAmqpQueueAccessorCallback<C> callback) {
		declareExchange(callback);
	}

	protected abstract void finishRegister(
			IAmqpQueueAccessorCallback<C> callback);

	/**
	 * A callback handler for the open connection request.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class ConnectionOpenHandler implements
			IOperationCompletionHandler<Boolean> {

		IResourceAccessorCallback<C> callback;

		public ConnectionOpenHandler(IResourceAccessorCallback<C> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
					AmqpQueueAccessor.this.cloudlet, result);
			synchronized (AmqpQueueAccessor.this.monitor) {
				this.callback.initializeSucceeded(
						AmqpQueueAccessor.this.cloudletContext, arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.READY;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
					AmqpQueueAccessor.this.cloudlet, error);
			this.callback.initializeFailed(
					AmqpQueueAccessor.this.cloudletContext, arguments);
		}

	}

	/**
	 * A callback handler for the open close request.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class ConnectionCloseHandler implements
			IOperationCompletionHandler<Boolean> {

		IResourceAccessorCallback<C> callback;

		public ConnectionCloseHandler(IResourceAccessorCallback<C> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
					AmqpQueueAccessor.this.cloudlet, result);
			synchronized (AmqpQueueAccessor.this.monitor) {
				this.callback.destroySucceeded(
						AmqpQueueAccessor.this.cloudletContext, arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.DESTROYED;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			CallbackArguments<C> arguments = new OperationResultCallbackArguments<C, Boolean>(
					AmqpQueueAccessor.this.cloudlet, error);
			this.callback.destroyFailed(AmqpQueueAccessor.this.cloudletContext,
					arguments);
		}

	}

}
