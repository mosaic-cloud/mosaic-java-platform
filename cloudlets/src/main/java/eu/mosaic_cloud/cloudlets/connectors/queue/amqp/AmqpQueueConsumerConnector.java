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

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the cloudlet context object
 * @param <D>
 *            the type of the messages consumed by the cloudlet
 */
public class AmqpQueueConsumerConnector<C, D, E> extends
		AmqpQueueConnector<C, D> implements IAmqpQueueConsumerConnector<C, D> {

	private String consumer;
	private boolean autoAck;
	private IAmqpQueueConsumerConnectorCallback<C, D> callback;

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
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public AmqpQueueConsumerConnector(IConfiguration config,
			ICloudletController<C> cloudlet, Class<D> dataClass,
			DataEncoder<D> encoder) {
		super(config, cloudlet, dataClass, true, encoder);
		synchronized (this.monitor) {
			String specification = ConfigProperties
					.getString("AmqpQueueConnector.4") + "." + ConfigProperties.getString("AmqpQueueConnector.10"); //$NON-NLS-1$
			this.autoAck = ConfigUtils.resolveParameter(config, specification,
					Boolean.class, false);
		}
	}

	@Override
	public CallbackCompletion<Void> initialize(IConnectorCallback<C> callback, C context,
			ThreadingContext threading) {
		if (callback instanceof IAmqpQueueConsumerConnectorCallback) {
			super.initialize(callback, context, threading);
			this.callback = (IAmqpQueueConsumerConnectorCallback<C, D>) callback;
		} else {
			IllegalArgumentException e = new IllegalArgumentException(
					"The callback argument must be of type " //$NON-NLS-1$
							+ IAmqpQueueConsumerConnectorCallback.class
									.getCanonicalName());
			IAmqpQueueConsumerConnectorCallback<C, D> proxy = this.cloudlet
					.buildCallbackInvoker(this.callback,
							IAmqpQueueConsumerConnectorCallback.class);
			CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
					AmqpQueueConsumerConnector.this.cloudlet, e);
			proxy.initializeFailed(context, arguments);
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConnector#register()
	 */
	@Override
	public CallbackCompletion<Void> register() {
		synchronized (this.monitor) {
			// declare queue and in case of success register as consumer
			startRegister(this.callback);
		}
	}

	@Override
	protected void finishRegister(final IAmqpQueueConnectorCallback<C> callback) {
		IOperationCompletionHandler<String> cHandler = new IOperationCompletionHandler<String>() {

			@Override
			public void onSuccess(String result) {
				synchronized (AmqpQueueConsumerConnector.this.monitor) {
					AmqpQueueConsumerConnector.this.logger.trace(
							"AmqpQueueConsumerConnector: received consume response message, consumer="
									+ result);
					AmqpQueueConsumerConnector.this.consumer = result;
					AmqpQueueConsumerConnector.super.registered = true;
				}
			}

			@Override
			public <T extends Throwable> void onFailure(T error) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, String>(
						AmqpQueueConsumerConnector.super.cloudlet, error);
				callback.registerFailed(AmqpQueueConsumerConnector.super.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<String>> cHandlers = new ArrayList<IOperationCompletionHandler<String>>();
		cHandlers.add(cHandler);

		IAmqpQueueConsumerCallback consumerCallback = new AmqpConsumerCallback();
		getConnector().consume(
				this.queue,
				this.consumer,
				super.exclusive,
				this.autoAck,
				null,
				cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler),
				this.cloudlet.buildCallbackInvoker(consumerCallback,
						IAmqpQueueConsumerCallback.class));
	}

	@Override
	public CallbackCompletion<Void> unregister() {
		synchronized (this.monitor) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {
	
				@Override
				public void onSuccess(Boolean result) {
					synchronized (AmqpQueueConsumerConnector.this.monitor) {
						AmqpQueueConsumerConnector.super.registered = false;
					}
				}
	
				@Override
				public <T extends Throwable> void onFailure(T error) {
					CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
							AmqpQueueConsumerConnector.super.cloudlet, error);
					AmqpQueueConsumerConnector.this.callback.unregisterFailed(
							AmqpQueueConsumerConnector.super.cloudletContext, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> cHandlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			cHandlers.add(cHandler);
			super.getConnector().cancel(this.consumer, cHandlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	/**
	 * Acknowledges a message.
	 * 
	 * @param message
	 *            the message to acknowledge
	 */
	@Override
	public CallbackCompletion<Void> acknowledge(AmqpQueueConsumeMessage<D> message) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConsumerConnector.super.cloudlet, result);
				AmqpQueueConsumerConnector.this.callback.acknowledgeSucceeded(
						AmqpQueueConsumerConnector.super.cloudletContext, arguments);
			}

			@Override
			public <T extends Throwable> void onFailure(T error) {
				CallbackArguments<C> arguments = new GenericCallbackCompletionArguments<C, Boolean>(
						AmqpQueueConsumerConnector.super.cloudlet, error);
				AmqpQueueConsumerConnector.this.callback.acknowledgeFailed(
						AmqpQueueConsumerConnector.super.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		AmqpInboundMessage inMssg = message.getMessage();
		super.getConnector().ack(inMssg.getDelivery(), false, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	/**
	 * Handler to be called when the queue consumer receives a message. Methods
	 * defined in this interface are called by the connector when one of the
	 * consume messages is received from the driver.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class AmqpConsumerCallback implements IAmqpQueueConsumerCallback {

		@Override
		public CallbackCompletion<Void> handleCancelOk(String consumerTag) {
			AmqpQueueConsumerConnector.this.logger.trace(
					"AmqpQueueConsumerConnector: received CANCEL ok message.");
			if (!AmqpQueueConsumerConnector.super.registered) {
				return;
			}
			CallbackArguments<C> arguments = new CallbackArguments<C>(
					AmqpQueueConsumerConnector.this.cloudlet);
			AmqpQueueConsumerConnector.this.callback.unregisterSucceeded(
					AmqpQueueConsumerConnector.this.cloudletContext, arguments);
			AmqpQueueConsumerConnector.super.registered = false;
		}

		@Override
		public CallbackCompletion<Void> handleConsumeOk(String consumerTag) {
			if (AmqpQueueConsumerConnector.super.registered) {
				return;
			}
			AmqpQueueConsumerConnector.this.logger.trace(
					"AmqpQueueConsumerConnector: received CONSUME ok message.");
			AmqpQueueConsumerConnector.this.consumer = consumerTag;
			CallbackArguments<C> arguments = new CallbackArguments<C>(
					AmqpQueueConsumerConnector.this.cloudlet);
			AmqpQueueConsumerConnector.this.callback.registerSucceeded(
					AmqpQueueConsumerConnector.this.cloudletContext, arguments);
			AmqpQueueConsumerConnector.super.registered = true;
		}

		@Override
		public CallbackCompletion<Void> handleDelivery(AmqpInboundMessage message) {
			D data;
			try {
				data = AmqpQueueConsumerConnector.this.dataEncoder.decode(message
						.getData());
				AmqpQueueConsumeMessage<D> mssg = new AmqpQueueConsumeMessage<D>(
						AmqpQueueConsumerConnector.this, message, data);
				AmqpQueueConsumeCallbackArguments<C, D> arguments = new AmqpQueueConsumeCallbackArguments<C, D>(
						AmqpQueueConsumerConnector.this.cloudlet, mssg);
				AmqpQueueConsumerConnector.this.callback.consume(
						AmqpQueueConsumerConnector.this.cloudletContext, arguments);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
			}
		}

		@Override
		public CallbackCompletion<Void> handleShutdownSignal(String consumerTag, String message) {

		}

	}

}
