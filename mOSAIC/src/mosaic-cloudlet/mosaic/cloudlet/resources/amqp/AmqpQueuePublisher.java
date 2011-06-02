package mosaic.cloudlet.resources.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.cloudlet.core.OperationResultCallbackArguments;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.connector.queue.AmqpOutboundMessage;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;

/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state object
 * @param <D>
 *            the type of the messages published by the cloudlet
 */
public class AmqpQueuePublisher<S, D extends Object> extends
		AmqpQueueAccessor<S, D> implements IAmqpQueuePublisher<S, D> {

	private String exchange;
	private String routingKey;
	private boolean mandatory;
	private boolean immediate;
	private boolean durable;
	private IAmqpQueuePublisherCallback<S, D> callback;

	/**
	 * Creates a new AMQP publisher.
	 * 
	 * @param config
	 *            configuration data required by the accessor:
	 *            <ul>
	 *            <li>amqp.publisher.exchange - the exchange to publish the
	 *            messages to</li>
	 *            <li>amqp.publisher.routing_key - the routing key of the
	 *            messages</li>
	 *            <li>amqp.publisher.manadatory - true if we are requesting a
	 *            mandatory publish</li>
	 *            <li>amqp.publisher.immediate - true if we are requesting an
	 *            immediate publish</li>
	 *            <li>amqp.publisher.durable - true if messages must not be lost
	 *            even if server shutdowns unexpectedly</li>
	 *            </ul>
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param dataClass
	 *            the type of the published messages
	 */
	public AmqpQueuePublisher(IConfiguration config,
			ICloudletController<S> cloudlet, Class<D> dataClass) {
		super(config, cloudlet, dataClass);
		synchronized (this) {
			this.exchange = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueuePublisher.0"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.routingKey = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueuePublisher.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			this.mandatory = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueuePublisher.2"), Boolean.class, false); //$NON-NLS-1$
			this.immediate = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueuePublisher.3"), Boolean.class, false); //$NON-NLS-1$
			this.durable = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("AmqpQueuePublisher.4"), Boolean.class, false); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.cloudlet.resources.amqp.AmqpQueueAccessor#initialize(mosaic.cloudlet
	 * .resources.IResourceAccessorCallback, java.lang.Object)
	 */
	public void initialize(IResourceAccessorCallback<S> callback, S state) {
		synchronized (this) {
			if (callback instanceof IAmqpQueuePublisherCallback) {
				super.initialize(callback, state);
				this.callback = (IAmqpQueuePublisherCallback<S, D>) callback;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(
						"The callback argument must be of type " //$NON-NLS-1$
								+ IAmqpQueuePublisherCallback.class
										.getCanonicalName());
				@SuppressWarnings("unchecked")
				IAmqpQueuePublisherCallback<S, D> proxy = this.cloudlet
						.buildCallbackInvoker(this.callback,
								IAmqpQueuePublisherCallback.class);
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueuePublisher.this.cloudlet, e);
				proxy.initializeFailed(state, arguments);
				throw e;
			}
		}
	}

	@Override
	public void register() {
		synchronized (this) {
			CallbackArguments<S> arguments = new CallbackArguments<S>(
					AmqpQueuePublisher.super.cloudlet);
			this.callback.registerSucceeded(
					AmqpQueuePublisher.this.cloudletState, arguments);
		}
	}

	@Override
	public void unregister() {
		synchronized (this) {
			CallbackArguments<S> arguments = new CallbackArguments<S>(
					AmqpQueuePublisher.super.cloudlet);
			this.callback.unregisterSucceeded(
					AmqpQueuePublisher.this.cloudletState, arguments);
		}
	}

	@Override
	public void publish(D data, final Object token) {
		synchronized (this) {
			try {
				byte[] sData = SerDesUtils.toBytes(data);
				final AmqpOutboundMessage message = new AmqpOutboundMessage(
						this.exchange, this.routingKey, sData, mandatory,
						immediate, durable);

				IOperationCompletionHandler<Boolean> cHandler = new PublishCompletionHandler(
						message, token);
				List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
				handlers.add(cHandler);

				super.getConnector().publish(message, handlers,
						this.cloudlet.getResponseInvocationHandler(cHandler));
			} catch (IOException e) {
				@SuppressWarnings("unchecked")
				IAmqpQueuePublisherCallback<S, D> proxy = this.cloudlet
						.buildCallbackInvoker(this.callback,
								IAmqpQueuePublisherCallback.class);
				AmqpQueuePublishMessage<D> pMessage = new AmqpQueuePublishMessage<D>(
						AmqpQueuePublisher.this, null, token);
				AmqpQueuePublishCallbackArguments<S, D> arguments = new AmqpQueuePublishCallbackArguments<S, D>(
						AmqpQueuePublisher.this.cloudlet, pMessage);
				proxy.publishFailed(AmqpQueuePublisher.this.cloudletState,
						arguments);
				ExceptionTracer.traceRethrown(e);
			}
		}

	}

	final class PublishCompletionHandler implements
			IOperationCompletionHandler<Boolean> {
		private AmqpQueuePublishCallbackArguments<S, D> arguments;

		public PublishCompletionHandler(AmqpOutboundMessage message,
				Object token) {
			AmqpQueuePublishMessage<D> pMessage = new AmqpQueuePublishMessage<D>(
					AmqpQueuePublisher.this, message, token);
			this.arguments = new AmqpQueuePublishCallbackArguments<S, D>(
					AmqpQueuePublisher.super.cloudlet, pMessage);
		}

		@Override
		public void onSuccess(Boolean result) {
			callback.publishSucceeded(AmqpQueuePublisher.super.cloudletState,
					this.arguments);
		}

		@Override
		public <E extends Throwable> void onFailure(E error) {
			callback.publishFailed(AmqpQueuePublisher.super.cloudletState,
					this.arguments);
		}
	}

}
