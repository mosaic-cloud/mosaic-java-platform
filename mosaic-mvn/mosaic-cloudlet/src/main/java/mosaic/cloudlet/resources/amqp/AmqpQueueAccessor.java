package mosaic.cloudlet.resources.amqp;

import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ContainerException;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.cloudlet.resources.ResourceStatus;
import mosaic.cloudlet.runtime.ContainerComponentCallbacks.ResourceType;
import mosaic.cloudlet.runtime.ResourceFinder;
import mosaic.connector.queue.amqp.AmqpConnector;
import mosaic.connector.queue.amqp.IAmqpQueueConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.DataEncoder;
import mosaic.driver.queue.amqp.AmqpExchangeType;

/**
 * Base accessor class for AMQP queuing systems.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using the accessor
 * @param <D>
 *            the type of messages processed by the accessor
 */
public abstract class AmqpQueueAccessor<S, D extends Object> implements
		IAmqpQueueAccessor<S> {
	private IConfiguration configuration;
	protected ICloudletController<S> cloudlet;
	protected S cloudletState;
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
			ICloudletController<S> cloudlet, Class<D> dataClass,
			boolean consumer, DataEncoder<D> encoder) {
		this.configuration = config;
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
		if (!type.equals("") && AmqpExchangeType.valueOf(type) != null)
			this.exchangeType = AmqpExchangeType.valueOf(type);
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
		MosaicLogger.getLogger().trace(
				"Queue accessor for exchange '" + this.exchange
						+ "', routing key '" + this.routingKey
						+ "' and queue '" + this.queue + "'");
	}

	@Override
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		synchronized (this) {
			@SuppressWarnings("unchecked")
			IResourceAccessorCallback<S> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IResourceAccessorCallback.class);
			try {
				this.status = ResourceStatus.INITIALIZING;
				this.cloudletState = state;
				if (!ResourceFinder.getResourceFinder().findResource(
						ResourceType.AMQP, configuration))
					throw new ContainerException(
							"Cannot find a resource of type "
									+ ResourceType.AMQP.toString());
				this.connector = AmqpConnector.create(this.configuration);

				// IOperationCompletionHandler<Boolean> cHandler = new
				// ConnectionOpenHandler(
				// callback);
				// List<IOperationCompletionHandler<Boolean>> handlers = new
				// ArrayList<IOperationCompletionHandler<Boolean>>();
				// handlers.add(cHandler);
				// this.connector.openConnection(handlers,
				// this.cloudlet.getResponseInvocationHandler(cHandler));

				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, true);
				proxy.initializeSucceeded(AmqpQueueAccessor.this.cloudletState,
						arguments);
				this.status = ResourceStatus.INITIALIZED;
			} catch (Throwable e) {
				e.printStackTrace();
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, e);
				proxy.initializeFailed(state, arguments);
				ExceptionTracer.traceDeferred(e);
			}
		}
	}

	@Override
	public void destroy(IResourceAccessorCallback<S> callback) {
		synchronized (this) {
			this.status = ResourceStatus.DESTROYING;
			@SuppressWarnings("unchecked")
			IResourceAccessorCallback<S> proxy = this.cloudlet
					.buildCallbackInvoker(callback,
							IResourceAccessorCallback.class);
			// IOperationCompletionHandler<Boolean> cHandler = new
			// ConnectionCloseHandler(
			// callback);
			// List<IOperationCompletionHandler<Boolean>> handlers = new
			// ArrayList<IOperationCompletionHandler<Boolean>>();
			// handlers.add(cHandler);
			// connector.closeConnection(handlers,
			// this.cloudlet.getResponseInvocationHandler(cHandler));
			try {
				MosaicLogger.getLogger().trace(
						"AmqpQueueAccessor is destroying the connector...");
				this.connector.destroy();
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, true);
				proxy.destroySucceeded(this.cloudletState, arguments);
				MosaicLogger.getLogger().trace(
						"AmqpQueueAccessor destroyed successfully.");
			} catch (Throwable e) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, e);
				proxy.destroyFailed(this.cloudletState, arguments);
				ExceptionTracer.traceDeferred(e);
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

	protected void declareExchange(final IAmqpQueueAccessorCallback<S> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.declareQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletState,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareExchange(this.exchange, this.exchangeType,
				this.durable, this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void declareQueue(final IAmqpQueueAccessorCallback<S> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.bindQueue(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletState,
						arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		cHandlers.add(cHandler);

		getConnector().declareQueue(this.queue, this.exclusive, this.durable,
				this.autoDelete, this.passive, cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));

	}

	private void bindQueue(final IAmqpQueueAccessorCallback<S> callback) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				AmqpQueueAccessor.this.finishRegister(callback);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.registerFailed(AmqpQueueAccessor.this.cloudletState,
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

	protected void startRegister(IAmqpQueueAccessorCallback<S> callback) {
		declareExchange(callback);
	}

	protected abstract void finishRegister(
			IAmqpQueueAccessorCallback<S> callback);

	/**
	 * A callback handler for the open connection request.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class ConnectionOpenHandler implements
			IOperationCompletionHandler<Boolean> {
		IResourceAccessorCallback<S> callback;

		public ConnectionOpenHandler(IResourceAccessorCallback<S> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, result);
				this.callback.initializeSucceeded(
						AmqpQueueAccessor.this.cloudletState, arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.READY;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, error);
				this.callback.initializeFailed(
						AmqpQueueAccessor.this.cloudletState, arguments);
			}
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
		IResourceAccessorCallback<S> callback;

		public ConnectionCloseHandler(IResourceAccessorCallback<S> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void onSuccess(Boolean result) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, result);
				this.callback.destroySucceeded(
						AmqpQueueAccessor.this.cloudletState, arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.DESTROYED;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, error);
				this.callback.destroyFailed(
						AmqpQueueAccessor.this.cloudletState, arguments);
			}
		}

	}

}
