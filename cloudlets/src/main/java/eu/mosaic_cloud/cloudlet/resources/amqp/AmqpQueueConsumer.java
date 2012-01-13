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
package eu.mosaic_cloud.cloudlet.resources.amqp;

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlet.ConfigProperties;
import eu.mosaic_cloud.cloudlet.core.CallbackArguments;
import eu.mosaic_cloud.cloudlet.core.ICloudletController;
import eu.mosaic_cloud.cloudlet.core.OperationResultCallbackArguments;
import eu.mosaic_cloud.cloudlet.resources.IResourceAccessorCallback;
import eu.mosaic_cloud.connector.queue.amqp.IAmqpConsumerCallback;
import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.utils.DataEncoder;
import eu.mosaic_cloud.driver.queue.amqp.AmqpInboundMessage;


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

	private String consumer;
	private boolean autoAck;
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
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public AmqpQueueConsumer(IConfiguration config,
			ICloudletController<S> cloudlet, Class<D> dataClass,
			DataEncoder<D> encoder) {
		super(config, cloudlet, dataClass, true, encoder);
		String specification = ConfigProperties
				.getString("AmqpQueueAccessor.4") + "." + ConfigProperties.getString("AmqpQueueConsumer.0"); //$NON-NLS-1$
		this.autoAck = ConfigUtils.resolveParameter(config, specification,
				Boolean.class, false);
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
	 * @see eu.mosaic_cloud.cloudlet.resources.amqp.IAmqpQueueAccessor#register()
	 */
	@Override
	public void register() {
		// declare queue and in case of success register as consumer
		synchronized (this) {
			startRegister(this.callback);
		}
	}

	@Override
	protected void finishRegister(final IAmqpQueueAccessorCallback<S> callback) {
		IOperationCompletionHandler<String> cHandler = new IOperationCompletionHandler<String>() {

			@Override
			public void onSuccess(String result) {
				synchronized (AmqpQueueConsumer.this) {
					//					if (AmqpQueueConsumer.super.registered)
					//						return;
					MosaicLogger.getLogger().trace(
							"AmqpQueueConsumer: received consume response message, consumer="
									+ result);
					AmqpQueueConsumer.this.consumer = result;
					//					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
					//							AmqpQueueConsumer.super.cloudlet, result);
					//					AmqpQueueConsumer.this.callback.registerSucceeded(
					//							AmqpQueueConsumer.this.cloudletState, arguments);
					AmqpQueueConsumer.super.registered = true;
				}
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, String>(
						AmqpQueueConsumer.super.cloudlet, error);
				callback.registerFailed(AmqpQueueConsumer.super.cloudletState,
						arguments);
			}
		};
		List<IOperationCompletionHandler<String>> cHandlers = new ArrayList<IOperationCompletionHandler<String>>();
		cHandlers.add(cHandler);

		IAmqpConsumerCallback consumerCallback = new AmqpConsumerCallback();
		getConnector().consume(
				this.queue,
				this.consumer,
				super.exclusive,
				this.autoAck,
				null,
				cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler),
				this.cloudlet.buildCallbackInvoker(consumerCallback,
						IAmqpConsumerCallback.class));
	}

	@Override
	public void unregister() {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@Override
				public void onSuccess(Boolean result) {
					synchronized (AmqpQueueConsumer.this) {
						// if (!AmqpQueueConsumer.super.registered)
						// return;
						// CallbackArguments<S> arguments = new
						// OperationResultCallbackArguments<S, Boolean>(
						// AmqpQueueConsumer.super.cloudlet, result);
						// AmqpQueueConsumer.this.callback
						// .unregisterSucceeded(
						// AmqpQueueConsumer.this.cloudletState,
						// arguments);
						AmqpQueueConsumer.super.registered = false;
					}
				}

				@Override
				public <E extends Throwable> void onFailure(E error) {
					CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
							AmqpQueueConsumer.super.cloudlet, error);
					AmqpQueueConsumer.this.callback.unregisterFailed(
							AmqpQueueConsumer.super.cloudletState, arguments);
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
	public void acknowledge(AmqpQueueConsumeMessage<D> message) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueConsumer.super.cloudlet, result);
				AmqpQueueConsumer.this.callback.acknowledgeSucceeded(
						AmqpQueueConsumer.super.cloudletState, arguments);
			}

			@Override
			public <E extends Throwable> void onFailure(E error) {
				CallbackArguments<S> arguments = new OperationResultCallbackArguments<S, Boolean>(
						AmqpQueueConsumer.super.cloudlet, error);
				AmqpQueueConsumer.this.callback.acknowledgeFailed(
						AmqpQueueConsumer.super.cloudletState, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		// if (!this.autoAck) {
		AmqpInboundMessage inMssg = message.getMessage();
		super.getConnector().ack(inMssg.getDelivery(), false, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
		// } else {
		// CallbackArguments<S> arguments = new
		// OperationResultCallbackArguments<S, Boolean>(
		// AmqpQueueConsumer.super.cloudlet, true);
		// callback.acknowledgeSucceeded(
		// AmqpQueueConsumer.super.cloudletState, arguments);
		// }
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
			MosaicLogger.getLogger().trace(
					"AmqpQueueConsumer: received CANCEL ok message.");
			if (!AmqpQueueConsumer.super.registered)
				return;
			CallbackArguments<S> arguments = new CallbackArguments<S>(
					AmqpQueueConsumer.this.cloudlet);
			AmqpQueueConsumer.this.callback.unregisterSucceeded(
					AmqpQueueConsumer.this.cloudletState, arguments);
			AmqpQueueConsumer.super.registered = false;
		}

		@Override
		public void handleConsumeOk(String consumerTag) {
			if (AmqpQueueConsumer.super.registered)
				return;
			MosaicLogger.getLogger().trace(
					"AmqpQueueConsumer: received CONSUME ok message.");
			AmqpQueueConsumer.this.consumer = consumerTag;
			CallbackArguments<S> arguments = new CallbackArguments<S>(
					AmqpQueueConsumer.this.cloudlet);
			AmqpQueueConsumer.this.callback.registerSucceeded(
					AmqpQueueConsumer.this.cloudletState, arguments);
			AmqpQueueConsumer.super.registered = true;
		}

		@Override
		public void handleDelivery(AmqpInboundMessage message) {
			D data;
			try {
				data = AmqpQueueConsumer.this.dataEncoder.decode(message
						.getData());
				AmqpQueueConsumeMessage<D> mssg = new AmqpQueueConsumeMessage<D>(
						AmqpQueueConsumer.this, message, data);
				AmqpQueueConsumeCallbackArguments<S, D> arguments = new AmqpQueueConsumeCallbackArguments<S, D>(
						AmqpQueueConsumer.this.cloudlet, mssg);
				AmqpQueueConsumer.this.callback.consume(
						AmqpQueueConsumer.this.cloudletState, arguments);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
			}
		}

		@Override
		public void handleShutdownSignal(String consumerTag, String message) {

		}

	}

}
