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
package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.core.ConnectorException;
import eu.mosaic_cloud.cloudlets.connectors.core.ConnectorStatus;
import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.runtime.CloudletComponentCallbacks.ResourceType;
import eu.mosaic_cloud.cloudlets.runtime.CloudletComponentResourceFinder;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
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
public abstract class AmqpQueueConnector<C, D extends Object> implements
		IAmqpQueueConnector<C> {

	private IConfiguration configuration;
	protected ICloudletController<C> cloudlet;
	protected C cloudletContext;
	private ConnectorStatus status;
	private eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConnector connector;
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
	public AmqpQueueConnector(IConfiguration config,
			ICloudletController<C> cloudlet, Class<D> dataClass,
			boolean consumer, DataEncoder<D> encoder) {
		synchronized (this.monitor) {
			this.configuration = config;
			this.logger=MosaicLogger.createLogger(this);
			this.cloudlet = cloudlet;
			this.status = ConnectorStatus.CREATED;
			this.dataClass = dataClass;
			this.dataEncoder = encoder;
			this.registered = false;
			String specification = ConfigProperties
					.getString("AmqpQueueConnector.3"); //$NON-NLS-1$
			if (consumer) {
				specification = ConfigProperties.getString("AmqpQueueConnector.4"); //$NON-NLS-1$
			}
			IConfiguration accessorConfig = config
					.spliceConfiguration(ConfigurationIdentifier
							.resolveRelative(specification));
			this.configuration = accessorConfig;
			this.exchange = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.0"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.routingKey = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.queue = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.2"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			String type = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.5"), String.class, "").toUpperCase();//$NON-NLS-1$ //$NON-NLS-2$
			if (!type.equals("") && (AmqpExchangeType.valueOf(type) != null)) {
				this.exchangeType = AmqpExchangeType.valueOf(type);
			}
			this.exclusive = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.6"), Boolean.class, true); //$NON-NLS-1$ 
			this.autoDelete = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.7"), Boolean.class, true); //$NON-NLS-1$
			this.passive = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.8"), Boolean.class, false); //$NON-NLS-1$ 
			this.durable = ConfigUtils
					.resolveParameter(
							accessorConfig,
							ConfigProperties.getString("AmqpQueueConnector.9"), Boolean.class, false); //$NON-NLS-1$ 
			this.logger.trace(
					"Queue accessor for exchange '" + this.exchange
							+ "', routing key '" + this.routingKey
							+ "' and queue '" + this.queue + "'");
		}
	}

	@Override
	public void initialize(IConnectorCallback<C> callback, C context,
			ThreadingContext threading) {
		synchronized (this.monitor) {
			@SuppressWarnings("unchecked")
			IConnectorCallback<C> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IConnectorCallback.class);
			try {
				this.status = ConnectorStatus.INITIALIZING;
				this.cloudletContext = context;
				if (!CloudletComponentResourceFinder.getResourceFinder().findResource(
						ResourceType.AMQP, this.configuration)) {
					throw new ConnectorException(
							"Cannot find a resource of type "
									+ ResourceType.AMQP.toString());
				}
				this.connector = eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConnector.create(this.configuration,
						threading);
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConnector.this.cloudlet, true);
				proxy.initializeSucceeded(AmqpQueueConnector.this.cloudletContext,
						arguments);
				this.status = ConnectorStatus.INITIALIZED;
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConnector.this.cloudlet, e);
				proxy.initializeFailed(context, arguments);
			}
		}
	}

	@Override
	public void destroy(IConnectorCallback<C> callback) {
		synchronized (this.monitor) {
			this.status = ConnectorStatus.DESTROYING;
			@SuppressWarnings("unchecked")
			IConnectorCallback<C> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IConnectorCallback.class);
			try {
				this.logger.trace(
						"AmqpQueueConnector is destroying the connector...");
				this.connector.destroy();
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConnector.this.cloudlet, true);
				proxy.destroySucceeded(this.cloudletContext, arguments);
				this.logger.trace(
						"AmqpQueueConnector destroyed successfully.");
			} catch (Throwable e) {
				ExceptionTracer.traceDeferred(e);
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConnector.this.cloudlet, e);
				proxy.destroyFailed(this.cloudletContext, arguments);
			}
			this.status = ConnectorStatus.DESTROYED;
		}
	}

	/**
	 * Returns the resource connector.
	 * 
	 * @return the resource connector
	 */
	protected eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConnector getConnector() {
		return this.connector;
	}

	@Override
	public ConnectorStatus getStatus() {
		return this.status;
	}

	protected void declareExchange(final IAmqpQueueConnectorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueConnector.this.declareQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, String>(
						AmqpQueueConnector.this.cloudlet, error);
				callback.registerFailed(AmqpQueueConnector.this.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareExchange(this.exchange, this.exchangeType,
				this.durable, this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void declareQueue(final IAmqpQueueConnectorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueConnector.this.bindQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, String>(
						AmqpQueueConnector.this.cloudlet, error);
				callback.registerFailed(AmqpQueueConnector.this.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareQueue(this.queue, this.exclusive, this.durable,
				this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void bindQueue(final IAmqpQueueConnectorCallback<C> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueConnector.this.finishRegister(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, String>(
						AmqpQueueConnector.this.cloudlet, error);
				callback.registerFailed(AmqpQueueConnector.this.cloudletContext,
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

	protected void startRegister(IAmqpQueueConnectorCallback<C> callback) {
		declareExchange(callback);
	}

	protected abstract void finishRegister(
			IAmqpQueueConnectorCallback<C> callback);

	/**
	 * A callback handler for the open connection request.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class ConnectionOpenHandler implements
			IOperationCompletionHandler<Boolean> {

		IConnectorCallback<C> callback;

		public ConnectionOpenHandler(IConnectorCallback<C> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
					AmqpQueueConnector.this.cloudlet, result);
			synchronized (AmqpQueueConnector.this.monitor) {
				this.callback.initializeSucceeded(
						AmqpQueueConnector.this.cloudletContext, arguments);
				AmqpQueueConnector.this.status = ConnectorStatus.READY;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
					AmqpQueueConnector.this.cloudlet, error);
			this.callback.initializeFailed(
					AmqpQueueConnector.this.cloudletContext, arguments);
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

		IConnectorCallback<C> callback;

		public ConnectionCloseHandler(IConnectorCallback<C> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
					AmqpQueueConnector.this.cloudlet, result);
			synchronized (AmqpQueueConnector.this.monitor) {
				this.callback.destroySucceeded(
						AmqpQueueConnector.this.cloudletContext, arguments);
				AmqpQueueConnector.this.status = ConnectorStatus.DESTROYED;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
					AmqpQueueConnector.this.cloudlet, error);
			this.callback.destroyFailed(AmqpQueueConnector.this.cloudletContext,
					arguments);
		}

	}

}
