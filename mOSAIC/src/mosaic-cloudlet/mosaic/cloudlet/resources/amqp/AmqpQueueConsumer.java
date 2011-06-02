package mosaic.cloudlet.resources.amqp;

import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.connector.queue.AmqpInboundMessage;
import mosaic.connector.queue.IAmqpConsumerCallback;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;

/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state object
 * @param <D>
 *            the type of the messages consumed by the cloudlet
 */
public class AmqpQueueConsumer<S, D extends Object> extends
		AmqpQueueAccessor<S, D> implements IAmqpQueueConsumer<S, D> {

	private String queue;
	private boolean autoAck;
	private String consumer;
	private boolean exclusive;
	private IAmqpQueueConsumerCallback<S, D> callback;

	/**
	 * Creates a new AMQP queue consumer.
	 * 
	 * @param config
	 *            configuration data required by the accessor:
	 *            <ul>
	 *            <li>amqp.consumer.queue - name of the queue from which to
	 *            consume messages</li>
	 *            <li>amqp.consumer.consumer_id - an if of this consumer</li>
	 *            <li>amqp.consumer.auto_ack - true if the server should
	 *            consider messages acknowledged once delivered; false if the
	 *            server should expect explicit acknowledgements</li>
	 *            <li>amqp.consumer.exclusive - true if this is an exclusive
	 *            consumer</li>
	 *            </ul>
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param dataClass
	 *            the type of the consumed messages
	 */
	public AmqpQueueConsumer(IConfiguration config,
			ICloudletController<S> cloudlet, Class<D> dataClass) {
		super(config, cloudlet, dataClass);
		synchronized (this) {
			this.queue = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueueConsumer.0"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.consumer = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueueConsumer.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.autoAck = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueueConsumer.2"), Boolean.class, false); //$NON-NLS-1$
			this.exclusive = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueueConsumer.3"), Boolean.class, false); //$NON-NLS-1$
		}
	}

	@Override
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		if (callback instanceof IAmqpQueueConsumerCallback) {
			super.initialize(callback, state);
			this.callback = (IAmqpQueueConsumerCallback<S, D>) callback;
		} else {
			IllegalArgumentException e = new IllegalArgumentException(
					"The callback argument must be of type " //$NON-NLS-1$
							+ IAmqpQueueConsumerCallback.class
									.getCanonicalName());

			@SuppressWarnings("unchecked")
			IAmqpQueueConsumerCallback<S, D> proxy = this.cloudlet
					.buildCallbackInvoker(this.callback,
							IAmqpQueueConsumerCallback.class);
			CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
					AmqpQueueConsumer.this.cloudlet, e);
			proxy.initializeFailed(state, arguments);
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.cloudlet.resources.amqp.IAmqpQueueAccessor#register()
	 */
	@Override
	public void register() {
		synchronized (this) {
			IOperationCompletionHandler<String> cHandler = new IOperationCompletionHandler<String>() {

				@Override
				public void onSuccess(String result) {
					// CallbackArguments<S> arguments = new
					// OperationResultCallbackArguments<S, String>(
					// AmqpQueueConsumer.super.cloudlet, result);
					// callback.registerSucceeded(
					// AmqpQueueConsumer.super.cloudletState, arguments);
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
							AmqpQueueConsumer.super.cloudlet, error);
					callback.registerFailed(
							AmqpQueueConsumer.super.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<String>> cHandlers = new ArrayList<IOperationCompletionHandler<String>>();
			cHandlers.add(cHandler);

			IAmqpConsumerCallback consumerCallback = new AmqpConsumerCallback();
			super.getConnector().consume(
					queue,
					consumer,
					exclusive,
					autoAck,
					null,
					cHandlers,
					this.cloudlet.getResponseInvocationHandler(cHandler),
					this.cloudlet.buildCallbackInvoker(consumerCallback,
							IAmqpConsumerCallback.class));
		}

	}

	@Override
	public void unregister() {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					// CallbackArguments<S> arguments = new
					// OperationResultCallbackArguments<S, Boolean>(
					// AmqpQueueConsumer.super.cloudlet, result);
					// callback.unregisterSucceeded(
					// AmqpQueueConsumer.super.cloudletState, arguments);
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
							AmqpQueueConsumer.super.cloudlet, error);
					callback.unregisterFailed(
							AmqpQueueConsumer.super.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			cHandlers.add(cHandler);
			super.getConnector().cancel(consumer, cHandlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	/**
	 * Acknowledges a message.
	 * 
	 * @param message
	 *            the message to acknowledge
	 */
	public void acknowledge(AmqpQueueConsumeMessage<D> message) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
							AmqpQueueConsumer.super.cloudlet, result);
					callback.acknowledgeSucceeded(
							AmqpQueueConsumer.super.cloudletState, arguments);
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
							AmqpQueueConsumer.super.cloudlet, error);
					callback.acknowledgeFailed(
							AmqpQueueConsumer.super.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			if (!this.autoAck) {
				AmqpInboundMessage inMssg = message.getMessage();
				super.getConnector().ack(inMssg.getDelivery(), false, handlers,
						this.cloudlet.getResponseInvocationHandler(cHandler));
			} else {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueConsumer.super.cloudlet, true);
				callback.acknowledgeSucceeded(
						AmqpQueueConsumer.super.cloudletState, arguments);
			}
		}
	}

	/**
	 * Handler to be called when the queue consumer receives a message. Methods
	 * defined in this interface are called by the connector when one of the
	 * consume messages is received from the driver.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class AmqpConsumerCallback implements IAmqpConsumerCallback {

		@Override
		public void handleCancelOk(String consumerTag) {
			synchronized (AmqpQueueConsumer.this) {
				CallbackArguments<S> arguments = new CallbackArguments<S>(
						AmqpQueueConsumer.this.cloudlet);
				AmqpQueueConsumer.this.callback.unregisterSucceeded(
						AmqpQueueConsumer.this.cloudletState, arguments);
			}
		}

		@Override
		public void handleConsumeOk(String consumerTag) {
			synchronized (AmqpQueueConsumer.this) {
				AmqpQueueConsumer.this.consumer = consumerTag;
				CallbackArguments<S> arguments = new CallbackArguments<S>(
						AmqpQueueConsumer.this.cloudlet);
				AmqpQueueConsumer.this.callback.registerSucceeded(
						AmqpQueueConsumer.this.cloudletState, arguments);
			}
		}

		@Override
		public void handleDelivery(AmqpInboundMessage message) {
			synchronized (AmqpQueueConsumer.this) {
				D data = AmqpQueueConsumer.this.deserializeMessage(message
						.getData());
				AmqpQueueConsumeMessage<D> mssg = new AmqpQueueConsumeMessage<D>(
						AmqpQueueConsumer.this, message, data);
				AmqpQueueConsumeCallbackArguments<S, D> arguments = new AmqpQueueConsumeCallbackArguments<S, D>(
						AmqpQueueConsumer.this.cloudlet, mssg);
				AmqpQueueConsumer.this.callback.consume(
						AmqpQueueConsumer.this.cloudletState, arguments);
			}
		}

		@Override
		public void handleShutdownSignal(String consumerTag, String message) {

		}

	}

}
