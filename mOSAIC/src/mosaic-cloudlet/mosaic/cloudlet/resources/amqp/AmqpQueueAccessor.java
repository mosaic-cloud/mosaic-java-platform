package mosaic.cloudlet.resources.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.cloudlet.resources.ResourceStatus;
import mosaic.connector.queue.AmqpConnector;
import mosaic.connector.queue.IAmqpQueue;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;

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
	private IAmqpQueue connector;
	private Class<D> dataClass;

	/**
	 * Creates a new AMQP resource accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param dataClass
	 *            the type of the consumed or produced messages
	 */
	public AmqpQueueAccessor(IConfiguration config,
			ICloudletController<S> cloudlet, Class<D> dataClass) {
		this.configuration = config;
		this.cloudlet = cloudlet;
		this.status = ResourceStatus.CREATED;
		this.dataClass = dataClass;
	}

	@Override
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		synchronized (this) {
			try {
				this.status = ResourceStatus.INITIALIZING;
				this.cloudletState = state;
				this.connector = AmqpConnector.create(configuration);
				IOperationCompletionHandler<Boolean> cHandler = new ConnectionOpenHandler(
						callback);
				List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
				handlers.add(cHandler);
				this.connector.openConnection(handlers, this.cloudlet
						.getResponseInvocationHandler(cHandler));
			} catch (IOException e) {
				@SuppressWarnings("unchecked")
				IResourceAccessorCallback<S> proxy = this.cloudlet
						.buildCallbackInvoker(callback,
								IResourceAccessorCallback.class);
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, e);
				proxy.initializeFailed(state, arguments);
				ExceptionTracer.traceRethrown(e);
			}
		}
	}

	@Override
	public void destroy(IResourceAccessorCallback<S> callback) {
		synchronized (this) {
			this.status = ResourceStatus.DESTROYING;
			IOperationCompletionHandler<Boolean> cHandler = new ConnectionCloseHandler(
					callback);
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			connector.closeConnection(handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
			connector.destroy();
		}
	}

	/**
	 * Returns the resource connector.
	 * 
	 * @return the resource connector
	 */
	protected IAmqpQueue getConnector() {
		return connector;
	}

	@Override
	public ResourceStatus getStatus() {
		return this.status;
	}

	protected D deserializeMessage(byte[] data) {
		D ob = null;
		try {
			ob = this.dataClass.cast(SerDesUtils.toObject(data));
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceRethrown(e);
		}
		return ob;
	}

	protected byte[] serializeMessage(D message) {
		byte[] bytes = null;
		try {
			bytes = SerDesUtils.toBytes(message);
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(e);
		}
		return bytes;
	}

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
				callback.initializeSucceeded(
						AmqpQueueAccessor.this.cloudletState, arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.READY;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.initializeFailed(AmqpQueueAccessor.this.cloudletState,
						arguments);
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
				callback.destroySucceeded(AmqpQueueAccessor.this.cloudletState,
						arguments);
				AmqpQueueAccessor.this.status = ResourceStatus.DESTROYED;
			}
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			synchronized (AmqpQueueAccessor.this) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueAccessor.this.cloudlet, error);
				callback.destroyFailed(AmqpQueueAccessor.this.cloudletState,
						arguments);
			}
		}

	}

}
