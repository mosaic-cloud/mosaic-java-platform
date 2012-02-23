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
package eu.mosaic_cloud.connectors.queue.amqp;

import java.util.ArrayList;
import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorCallback;

import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
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
 * @param <Context>
 *            the type of the cloudlet context object
 * @param <Message>
 *            the type of the messages consumed by the cloudlet
 */
public class AmqpQueueConsumerConnectorProxy<Message> extends
		AmqpQueueConnectorProxy {

	private String consumer;
	private boolean autoAck;
	private IAmqpQueueConsumerConnectorCallback<Context, Message, Extra> callback;

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
			ICloudletController<Context> cloudlet, Class<Message> dataClass,
			DataEncoder<Message> encoder) {
		super(config, cloudlet, dataClass, true, encoder);
		synchronized (this.monitor) {
			String specification = ConfigProperties
					.getString("AmqpQueueConnector.4") + "." + ConfigProperties.getString("AmqpQueueConnector.10"); //$NON-NLS-1$
			this.autoAck = ConfigUtils.resolveParameter(config, specification,
					Boolean.class, false);
		}
	}

	@Override
	public CallbackCompletion<Void> initialize(IConnectorCallback<Context> callback, Context context,
			ThreadingContext threading) {
		if (callback instanceof IAmqpQueueConsumerConnectorCallback) {
			super.initialize(callback, context, threading);
			this.callback = (IAmqpQueueConsumerConnectorCallback<Context, Message, Extra>) callback;
		} else {
			IllegalArgumentException e = new IllegalArgumentException(
					"The callback argument must be of type " //$NON-NLS-1$
							+ IAmqpQueueConsumerConnectorCallback.class
									.getCanonicalName());
			IAmqpQueueConsumerConnectorCallback<Context, Message, Extra> proxy = this.cloudlet
					.buildCallbackInvoker(this.callback,
							IAmqpQueueConsumerConnectorCallback.class);
			CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, Boolean>(
					AmqpQueueConsumerConnectorProxy.this.cloudlet, e);
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
	public CallbackCompletion<Void> register(IAmqpQueueConsumerCallback<Message> callback) {
		synchronized (this.monitor) {
			// declare queue and in case of success register as consumer
			startRegister(this.callback);
		}
	}

	@Override
	protected void finishRegister(final IAmqpQueueConnectorCallback<Context> callback) {
		IOperationCompletionHandler<String> cHandler = new IOperationCompletionHandler<String>() {

			@Override
			public void onSuccess(String result) {
				synchronized (AmqpQueueConsumerConnectorProxy.this.monitor) {
					AmqpQueueConsumerConnector.this.logger.trace(
							"AmqpQueueConsumerConnector: received consume response message, consumer="
									+ result);
					AmqpQueueConsumerConnector.this.consumer = result;
					AmqpQueueConsumerConnector.super.registered = true;
				}
			}

			@Override
			public void onFailure(Throwable error) {
				CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, String>(
						AmqpQueueConsumerConnectorProxy.super.cloudlet, error);
				callback.registerFailed(AmqpQueueConsumerConnectorProxy.super.cloudletContext,
						arguments);
			}
		};
		List<IOperationCompletionHandler<String>> cHandlers = new ArrayList<IOperationCompletionHandler<String>>();
		cHandlers.add(cHandler);

		IAmqpQueueRawConsumerCallback consumerCallback = new AmqpConsumerCallback();
		getConnector().consume(
				this.queue,
				this.consumer,
				super.exclusive,
				this.autoAck,
				cHandlers,
				this.cloudlet.getResponseInvocationHandler(cHandler),
				this.cloudlet.buildCallbackInvoker(consumerCallback,
						IAmqpQueueRawConsumerCallback.class));
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
				public void onFailure(Throwable error) {
					CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, Boolean>(
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
	public CallbackCompletion<Void> acknowledge(IAmqpQueueDeliveryToken delivery) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@Override
			public void onSuccess(Boolean result) {
				CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, Boolean>(
						AmqpQueueConsumerConnectorProxy.super.cloudlet, result);
				AmqpQueueConsumerConnectorProxy.this.callback.acknowledgeSucceeded(
						AmqpQueueConsumerConnectorProxy.super.cloudletContext, arguments);
			}

			@Override
			public void onFailure(Throwable error) {
				CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, Boolean>(
						AmqpQueueConsumerConnectorProxy.super.cloudlet, error);
				AmqpQueueConsumerConnectorProxy.this.callback.acknowledgeFailed(
						AmqpQueueConsumerConnectorProxy.super.cloudletContext, arguments);
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
	final class AmqpConsumerCallback implements IAmqpQueueRawConsumerCallback {

		@Override
		public CallbackCompletion<Void> handleCancelOk(String consumerTag) {
			AmqpQueueConsumerConnectorProxy.this.logger.trace(
					"AmqpQueueConsumerConnector: received CANCEL ok message.");
			if (!AmqpQueueConsumerConnectorProxy.super.registered) {
				return;
			}
			CallbackArguments<Context> arguments = new CallbackArguments<Context>(
					AmqpQueueConsumerConnectorProxy.this.cloudlet);
			AmqpQueueConsumerConnectorProxy.this.callback.unregisterSucceeded(
					AmqpQueueConsumerConnectorProxy.this.cloudletContext, arguments);
			AmqpQueueConsumerConnectorProxy.super.registered = false;
		}

		@Override
		public CallbackCompletion<Void> handleConsumeOk(String consumerTag) {
			if (AmqpQueueConsumerConnectorProxy.super.registered) {
				return;
			}
			AmqpQueueConsumerConnectorProxy.this.logger.trace(
					"AmqpQueueConsumerConnector: received CONSUME ok message.");
			AmqpQueueConsumerConnectorProxy.this.consumer = consumerTag;
			CallbackArguments<Context> arguments = new CallbackArguments<Context>(
					AmqpQueueConsumerConnectorProxy.this.cloudlet);
			AmqpQueueConsumerConnectorProxy.this.callback.registerSucceeded(
					AmqpQueueConsumerConnectorProxy.this.cloudletContext, arguments);
			AmqpQueueConsumerConnectorProxy.super.registered = true;
		}

		@Override
		public CallbackCompletion<Void> handleDelivery(AmqpInboundMessage message) {
			Message data;
			try {
				data = AmqpQueueConsumerConnectorProxy.this.dataEncoder.decode(message
						.getData());
				AmqpQueueConsumeMessage<Message> mssg = new AmqpQueueConsumeMessage<Message>(
						AmqpQueueConsumerConnectorProxy.this, message, data);
				AmqpQueueConsumeCallbackArguments<Context, Message, Extra> arguments = new AmqpQueueConsumeCallbackArguments<Context, Message, Extra>(
						AmqpQueueConsumerConnectorProxy.this.cloudlet, mssg);
				AmqpQueueConsumerConnectorProxy.this.callback.consume(
						AmqpQueueConsumerConnectorProxy.this.cloudletContext, arguments);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
			}
		}

		@Override
		public CallbackCompletion<Void> handleShutdownSignal(String consumerTag, String message) {

		}

	}

}
