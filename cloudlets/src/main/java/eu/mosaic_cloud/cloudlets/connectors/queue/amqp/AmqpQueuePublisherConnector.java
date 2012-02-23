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
import eu.mosaic_cloud.drivers.queue.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Context>
 *            the type of the cloudlet context object
 * @param <Message>
 *            the type of the messages published by the cloudlet
 */
public class AmqpQueuePublisherConnector<Context, Message> extends
		AmqpQueueConnector<Context, Message> implements IAmqpQueuePublisherConnector<Context, Message> {

	private IAmqpQueuePublisherConnectorCallback<Context, Message> callback;

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
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public AmqpQueuePublisherConnector(IConfiguration config,
			ICloudletController<Context> cloudlet, Class<Message> dataClass,
			DataEncoder<Message> encoder) {
		super(config, cloudlet, dataClass, false, encoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConnector#initialize
	 * (eu.mosaic_cloud.cloudlets .resources.IResourceAccessorCallback,
	 * java.lang.Object)
	 */
	@Override
	public CallbackCompletion<Void> initialize(IConnectorCallback<Context> callback, Context context,
			ThreadingContext threading) {
		synchronized (this.monitor) {
			if (callback instanceof IAmqpQueuePublisherConnectorCallback) {
				super.initialize(callback, context, threading);
				this.callback = (IAmqpQueuePublisherConnectorCallback<Context, Message>) callback;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(
						"The callback argument must be of type " //$NON-NLS-1$
								+ IAmqpQueuePublisherConnectorCallback.class
										.getCanonicalName());
				IAmqpQueuePublisherConnectorCallback<Context, Message> proxy = this.cloudlet
						.buildCallbackInvoker(this.callback,
								IAmqpQueuePublisherConnectorCallback.class);
				CallbackArguments<Context> arguments = new GenericCallbackCompletionArguments<Context, Boolean>(
						AmqpQueuePublisherConnector.this.cloudlet, e);
				proxy.initializeFailed(context, arguments);
				throw e;
			}
		}
	}

	@Override
	public CallbackCompletion<Void> register() {
		// declare queue and in case of success register as consumer
		synchronized (this.monitor) {
			startRegister(this.callback);
		}
	}

	@Override
	protected void finishRegister(IAmqpQueueConnectorCallback<Context> callback) {
		if (AmqpQueuePublisherConnector.super.registered) {
			return;
		}
		CallbackArguments<Context> arguments = new CallbackArguments<Context>(
				AmqpQueuePublisherConnector.super.cloudlet);
		this.callback.registerSucceeded(AmqpQueuePublisherConnector.this.cloudletContext,
				arguments);
		synchronized (this.monitor) {
			AmqpQueuePublisherConnector.super.registered = true;
		}

	}

	@Override
	public CallbackCompletion<Void> unregister() {
		synchronized (this.monitor) {
			if (!AmqpQueuePublisherConnector.super.registered) {
				return;
			}
			AmqpQueuePublisherConnector.super.registered = false;
		}
		CallbackArguments<Context> arguments = new CallbackArguments<Context>(
				AmqpQueuePublisherConnector.super.cloudlet);
		this.callback.unregisterSucceeded(
				AmqpQueuePublisherConnector.this.cloudletContext, arguments);
	}

	@Override
	public CallbackCompletion<Void> publish(Message data) {
		try {
			byte[] sData = this.dataEncoder.encode(data);
			final AmqpOutboundMessage message = new AmqpOutboundMessage(
					this.exchange, this.routingKey, sData, true, true, false,
					null);

			IOperationCompletionHandler<Boolean> cHandler = new PublishCompletionHandler(
					message);
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);

			super.getConnector().publish(message, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
			this.logger.trace(
					"AmqpQueuePublisherConnector - published message " + data);
		} catch (Exception e) {
			ExceptionTracer.traceDeferred(e);
			IAmqpQueuePublisherConnectorCallback<Context, Message> proxy = this.cloudlet
					.buildCallbackInvoker(this.callback,
							IAmqpQueuePublisherConnectorCallback.class);
			AmqpQueuePublishMessage<Message> pMessage = new AmqpQueuePublishMessage<Message>(
					AmqpQueuePublisherConnector.this, null);
			AmqpQueuePublishCallbackCompletionArguments<Context, Message> arguments = new AmqpQueuePublishCallbackCompletionArguments<Context, Message>(
					AmqpQueuePublisherConnector.this.cloudlet, pMessage);
			proxy.publishFailed(AmqpQueuePublisherConnector.this.cloudletContext,
					arguments);
		}

	}

	final class PublishCompletionHandler implements
			IOperationCompletionHandler<Boolean> {

		private AmqpQueuePublishCallbackCompletionArguments<Context, Message> arguments;

		public PublishCompletionHandler(AmqpOutboundMessage message) {
			AmqpQueuePublishMessage<Message> pMessage = new AmqpQueuePublishMessage<Message>(
					AmqpQueuePublisherConnector.this, message);
			this.arguments = new AmqpQueuePublishCallbackCompletionArguments<Context, Message>(
					AmqpQueuePublisherConnector.super.cloudlet, pMessage);
		}

		@Override
		public void onSuccess(Boolean result) {
			AmqpQueuePublisherConnector.this.callback.publishSucceeded(
					AmqpQueuePublisherConnector.super.cloudletContext, this.arguments);
		}

		@Override
		public void onFailure(Throwable error) {
			AmqpQueuePublisherConnector.this.callback.publishFailed(
					AmqpQueuePublisherConnector.super.cloudletContext, this.arguments);
		}
	}

}
