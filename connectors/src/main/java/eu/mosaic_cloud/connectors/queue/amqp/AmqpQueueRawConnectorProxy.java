/*
 * #%L
 * mosaic-connectors
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


import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.connectors.core.BaseConnectorProxy;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.CancelOkMessage;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.ConsumeOkMessage;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.ConsumeReply;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest.ExchangeType;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.DeliveryMessage;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.ServerCancelRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.ShutdownMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;


/**
 * Proxy for the driver for queuing systems implementing the AMQP protocol. This
 * is used by the {@link AmqpQueueRawConnector} to communicate with a AMQP
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpQueueRawConnectorProxy
		extends BaseConnectorProxy
		implements
			// NOPMD
			IAmqpQueueRawConnector
{ // NOPMD
	protected AmqpQueueRawConnectorProxy (final ConnectorConfiguration configuration)
	{
		super (configuration);
		this.pendingConsumers = new ConcurrentHashMap<String, IAmqpQueueRawConsumerCallback> ();
		this.transcript.traceDebugging ("created queue raw connector proxy.");
	}
	
	@Override
	public CallbackCompletion<Void> ack (final long delivery, final boolean multiple)
	{
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("acknowledging the message `%l` (with multiple `%b`) (with request token `%s`)...", Long.valueOf (delivery), Boolean.valueOf (multiple), token.getMessageId ());
		final AmqpPayloads.Ack.Builder requestBuilder = AmqpPayloads.Ack.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setDelivery (delivery);
		requestBuilder.setMultiple (multiple);
		final Message message = new Message (AmqpMessage.ACK, requestBuilder.build ());
		return (this.sendRequest (message, token, Void.class));
	}
	
	@Override
	public CallbackCompletion<Void> bindQueue (final String exchange, final String queue, final String routingKey)
	{
		Preconditions.checkNotNull (exchange);
		Preconditions.checkNotNull (queue);
		Preconditions.checkNotNull (routingKey);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("binding the queue `%s` to the exchange `%s` with the routing key `%s` (with request token `%s`)...", queue, exchange, routingKey, token.getMessageId ());
		final AmqpPayloads.BindQueueRequest.Builder requestBuilder = AmqpPayloads.BindQueueRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (exchange);
		requestBuilder.setQueue (queue);
		requestBuilder.setRoutingKey (routingKey);
		final Message message = new Message (AmqpMessage.BIND_QUEUE_REQUEST, requestBuilder.build ());
		return (this.sendRequest (message, token, Void.class));
	}
	
	@Override
	public CallbackCompletion<Void> cancel (final String consumer)
	{
		Preconditions.checkNotNull (consumer);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("canceling the consumer `%s` (with request token `%s`)...", consumer, token.getMessageId ());
		final AmqpPayloads.CancelRequest.Builder requestBuilder = AmqpPayloads.CancelRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setConsumer (consumer);
		final Message message = new Message (AmqpMessage.CANCEL_REQUEST, requestBuilder.build ());
		final CallbackCompletionDeferredFuture<Void> consumeFuture = CallbackCompletionDeferredFuture.create (Void.class);
		this.pendingRequests.register (consumer + "//cancel", consumeFuture);
		this.sendRequest (message, token, Void.class);
		return (consumeFuture.completion);
	}
	
	@Override
	public CallbackCompletion<Void> consume (final String queue, final String consumer, final boolean exclusive, final boolean autoAck, final IAmqpQueueRawConsumerCallback consumerCallback)
	{
		Preconditions.checkNotNull (queue);
		Preconditions.checkNotNull (consumer);
		Preconditions.checkNotNull (consumerCallback);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("registering the consumer `%s` for queue `%s` (with exclusive `%b`, auto-acknowledge `%b`, and callbacks `%{object:identity}`) (with request token `%s`)...", consumer, queue, Boolean.valueOf (exclusive), Boolean.valueOf (autoAck), consumerCallback, token.getMessageId ());
		final AmqpPayloads.ConsumeRequest.Builder requestBuilder = AmqpPayloads.ConsumeRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setConsumer (consumer);
		requestBuilder.setExclusive (exclusive);
		requestBuilder.setAutoAck (autoAck);
		requestBuilder.setExtra (ByteString.EMPTY);
		final Message message = new Message (AmqpMessage.CONSUME_REQUEST, requestBuilder.build ());
		this.pendingConsumers.put (consumer, consumerCallback);
		final CallbackCompletionDeferredFuture<Void> consumeFuture = CallbackCompletionDeferredFuture.create (Void.class);
		this.pendingRequests.register (consumer + "//consume", consumeFuture);
		// FIXME: how should we handle the next completion?
		this.sendRequest (message, token, Void.class);
		return (consumeFuture.completion);
	}
	
	@Override
	public CallbackCompletion<Void> declareExchange (final String name, final AmqpExchangeType type, final boolean durable, final boolean autoDelete, final boolean passive)
	{
		Preconditions.checkNotNull (name);
		Preconditions.checkNotNull (type);
		final CompletionToken token = this.generateToken ();
		final ExchangeType eType = AmqpPayloads.DeclareExchangeRequest.ExchangeType.valueOf (type.toString ().toUpperCase ());
		this.transcript.traceDebugging ("declaring the exchange `%s` of type `%s` (with durable `%b`, auto-delete `%b`, and passive `%b`) (with request token `%s`)...", name, type, Boolean.valueOf (durable), Boolean.valueOf (autoDelete), Boolean.valueOf (passive), token.getMessageId ());
		final AmqpPayloads.DeclareExchangeRequest.Builder requestBuilder = AmqpPayloads.DeclareExchangeRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (name);
		requestBuilder.setType (eType);
		requestBuilder.setDurable (durable);
		requestBuilder.setAutoDelete (autoDelete);
		requestBuilder.setPassive (passive);
		final Message message = new Message (AmqpMessage.DECL_EXCHANGE_REQUEST, requestBuilder.build ());
		return (this.sendRequest (message, token, Void.class));
	}
	
	@Override
	public CallbackCompletion<Void> declareQueue (final String queue, final boolean exclusive, final boolean durable, final boolean autoDelete, final boolean passive)
	{
		Preconditions.checkNotNull (queue);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("declaring the queue `%s` (with exclusive `%b`, durable `%b`, auto-delete `%b`, and passive `%b`) (with request token `%s`)...", queue, Boolean.valueOf (exclusive), Boolean.valueOf (durable), Boolean.valueOf (autoDelete), Boolean.valueOf (passive), Boolean.valueOf (passive), token.getMessageId ());
		final AmqpPayloads.DeclareQueueRequest.Builder requestBuilder = AmqpPayloads.DeclareQueueRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setExclusive (exclusive);
		requestBuilder.setDurable (durable);
		requestBuilder.setAutoDelete (autoDelete);
		requestBuilder.setPassive (passive);
		final Message message = new Message (AmqpMessage.DECL_QUEUE_REQUEST, requestBuilder.build ());
		return (this.sendRequest (message, token, Void.class));
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		this.transcript.traceDebugging ("destroying the proxy...");
		return (this.disconnect (null));
	}
	
	@Override
	public CallbackCompletion<Void> get (final String queue, final boolean autoAck)
	{
		Preconditions.checkNotNull (queue);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("pulling one message from the queue `%s` (with auto-acknowledge `%b`) (with request token `%s`)...", queue, Boolean.valueOf (autoAck), token.getMessageId ());
		final AmqpPayloads.GetRequest.Builder requestBuilder = AmqpPayloads.GetRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setAutoAck (autoAck);
		final Message message = new Message (AmqpMessage.GET_REQUEST, requestBuilder.build ());
		return (this.sendRequest (message, token, Void.class));
	}
	
	@Override
	public CallbackCompletion<Void> initialize ()
	{
		this.transcript.traceDebugging ("initializing the proxy...");
		return (this.connect (AmqpSession.CONNECTOR, new Message (AmqpMessage.ACCESS, null)));
	}
	
	@Override
	public CallbackCompletion<Void> publish (final AmqpOutboundMessage message)
	{
		Preconditions.checkNotNull (message);
		final String exchange = message.getExchange ();
		final String routingKey = message.getRoutingKey ();
		final byte[] data = message.getData ();
		final boolean mandatory = message.isMandatory ();
		final boolean immediate = message.isImmediate ();
		final boolean durable = message.isDurable ();
		final String contentType = message.getContentType ();
		final String contentEncoding = message.getContentEncoding ();
		final String correlation = message.getCorrelation ();
		final String callback = message.getCallback ();
		return (this.publish (data, exchange, routingKey, contentType, contentEncoding, mandatory, immediate, durable, correlation, callback));
	}
	
	public CallbackCompletion<Void> publish (final byte[] data, final String exchange, final String routingKey, final String contentType, final String contentEncoding, final boolean mandatory, final boolean immediate, final boolean durable, final String correlation, final String callback)
	{
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (exchange);
		Preconditions.checkNotNull (routingKey);
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("publishing a message (of size `%d`) to exchange `%s` with routing key `%s` (with content-type `%s`, content-encoding `%s`, mandatory `%b`, immediate `%b`, durable `%b`, correlation `%s`, and callback `%s`) (with request token `%s`)", Integer.valueOf (data.length), exchange, routingKey, contentType, contentEncoding, Boolean.valueOf (mandatory), Boolean.valueOf (immediate), Boolean.valueOf (durable), correlation, callback, token.getMessageId ());
		final AmqpPayloads.PublishRequest.Builder requestBuilder = AmqpPayloads.PublishRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (exchange);
		requestBuilder.setRoutingKey (routingKey);
		requestBuilder.setData (ByteString.copyFrom (data));
		requestBuilder.setMandatory (mandatory);
		requestBuilder.setImmediate (immediate);
		requestBuilder.setDurable (durable);
		if (contentType != null) {
			requestBuilder.setContentType (contentType);
		}
		// FIXME: content encoding is missing...
		// if (contentEncoding != null) {
		//	requestBuilder.setContentEncoding (contentEncoding);
		// }
		if (correlation != null) {
			requestBuilder.setCorrelationId (correlation);
		}
		if (callback != null) {
			requestBuilder.setReplyTo (callback);
		}
		final Message mssg = new Message (AmqpMessage.PUBLISH_REQUEST, requestBuilder.build ());
		return (this.sendRequest (mssg, token, Void.class));
	}
	
	@Override
	protected void processResponse (final Message message)
	{ // NOPMD
		final AmqpMessage amqpMessage = (AmqpMessage) message.specification;
		switch (amqpMessage) {
			case OK : {
				final IdlCommon.Ok okPayload = (Ok) message.payload;
				final CompletionToken token = okPayload.getToken ();
				this.transcript.traceDebugging ("processing the success (OK) response for pending request with token `%s`...", token.getMessageId ());
				this.pendingRequests.succeed (token.getMessageId (), null);
			}
				break;
			case NOK : {
				final IdlCommon.NotOk nokPayload = (NotOk) message.payload;
				final CompletionToken token = nokPayload.getToken ();
				this.transcript.traceDebugging ("processing the failure (NOK) response for pending request with token `%s`...", token.getMessageId ());
				// FIXME: ??? (I don't remember what the problem was...)
				this.pendingRequests.fail (token.getMessageId (), new Exception ("request failed"));
			}
				break;
			case ERROR : {
				final IdlCommon.Error errorPayload = (Error) message.payload;
				final CompletionToken token = errorPayload.getToken ();
				this.transcript.traceDebugging ("processing the failure (error) response for pending request with token `%s` with message `%s`...", token.getMessageId (), errorPayload.getErrorMessage ());
				this.pendingRequests.fail (token.getMessageId (), new Exception (errorPayload.getErrorMessage ()));
			}
				break;
			case CONSUME_REPLY : {
				final AmqpPayloads.ConsumeReply consumePayload = (ConsumeReply) message.payload;
				final CompletionToken token = consumePayload.getToken ();
				this.transcript.traceDebugging ("processing the success (consumer reply) response for pending request with token `%s`...", token.getMessageId ());
				this.pendingRequests.succeed (token.getMessageId (), null);
			}
				break;
			case CANCEL_OK : {
				final AmqpPayloads.CancelOkMessage cancelOkPayload = (CancelOkMessage) message.payload;
				final String consumerIdentifier = cancelOkPayload.getConsumerTag ();
				this.transcript.traceDebugging ("processing the cancelation of the consumer `%s` (by the client)...", consumerIdentifier);
				final IAmqpQueueRawConsumerCallback consumerCallback = this.pendingConsumers.remove (consumerIdentifier);
				Preconditions.checkNotNull (consumerCallback);
				consumerCallback.handleCancelOk (consumerIdentifier);
				this.pendingRequests.succeed (consumerIdentifier + "//cancel", null);
			}
				break;
			case SERVER_CANCEL : {
				final AmqpPayloads.ServerCancelRequest scancelPayload = (ServerCancelRequest) message.payload;
				final String consumerIdentifier = scancelPayload.getConsumerTag ();
				this.transcript.traceDebugging ("processing the cancelation of the consumer `%s` (by the server)...", consumerIdentifier);
				final IAmqpQueueRawConsumerCallback consumerCallback = this.pendingConsumers.remove (consumerIdentifier);
				Preconditions.checkNotNull (consumerCallback);
				consumerCallback.handleCancelOk (consumerIdentifier);
				if (this.pendingRequests.peekMaybe (consumerIdentifier + "//consume") != null)
					this.pendingRequests.cancel (consumerIdentifier + "//consume");
				if (this.pendingRequests.peekMaybe (consumerIdentifier + "//cancel") != null)
					this.pendingRequests.cancel (consumerIdentifier + "//cancel");
			}
				break;
			case CONSUME_OK : {
				final AmqpPayloads.ConsumeOkMessage consumeOkPayload = (ConsumeOkMessage) message.payload;
				// FIXME: missing token...
				// final CompletionToken token = cancelOkPayload.getToken ();
				final String consumerIdentifier = consumeOkPayload.getConsumerTag ();
				this.transcript.traceDebugging ("processing the registration for the consumer `%s` for pending request with token `%s`...", consumerIdentifier, null);
				final IAmqpQueueRawConsumerCallback consumerCallback = this.pendingConsumers.get (consumerIdentifier);
				consumerCallback.handleConsumeOk (consumerIdentifier);
				this.pendingRequests.succeed (consumerIdentifier + "//consume", null);
			}
				break;
			case DELIVERY : {
				final AmqpPayloads.DeliveryMessage delivery = (DeliveryMessage) message.payload;
				final String consumerIdentifier = delivery.getConsumerTag ();
				final long deliveryTag = delivery.getDeliveryTag ();
				final String exchange = delivery.getExchange ();
				final String routingKey = delivery.getRoutingKey ();
				final int deliveryMode = delivery.getDeliveryMode ();
				final boolean durable = deliveryMode == 2;
				final byte[] data = delivery.getData ().toByteArray ();
				final String contentType = delivery.hasContentType () ? delivery.getContentType () : null;
				// FIXME: content encoding is missing...
				// final String contentEncoding = delivery.hasContentEncoding () ? delivery.getContentEncoding () : null;
				final String contentEncoding = null;
				final String correlation = delivery.hasCorrelationId () ? delivery.getCorrelationId () : null;
				final String callback = delivery.hasReplyTo () ? delivery.getReplyTo () : null;
				this.transcript.traceDebugging ("processing a message delivery (of size `%d`) for the consumer `%s` from exchange `%s` with routing key `%s` (with content-type `%s`, content-encoding `%s`, durable `%b`, correlation `%s`, callback `%s`)...", Integer.valueOf (data.length), consumerIdentifier, exchange, routingKey, contentType, contentEncoding, Boolean.valueOf (durable), correlation, callback);
				final IAmqpQueueRawConsumerCallback consumerCallback = this.pendingConsumers.get (consumerIdentifier);
				Preconditions.checkNotNull (consumerIdentifier);
				final AmqpInboundMessage inboundMessage = new AmqpInboundMessage (consumerIdentifier, deliveryTag, exchange, routingKey, data, durable, callback, contentEncoding, contentType, correlation, null);
				consumerCallback.handleDelivery (inboundMessage);
			}
				break;
			case SHUTDOWN : {
				final AmqpPayloads.ShutdownMessage downPayload = (ShutdownMessage) message.payload;
				final String consumerIdentifier = downPayload.getConsumerTag ();
				final String shutdownSignal = downPayload.getMessage ();
				this.transcript.traceDebugging ("processing the cancelation of the consumer `%s` (by the server shutdown with signal `%s`)...", consumerIdentifier, shutdownSignal);
				final IAmqpQueueRawConsumerCallback consumerCallback = this.pendingConsumers.remove (consumerIdentifier);
				Preconditions.checkNotNull (consumerCallback);
				consumerCallback.handleShutdownSignal (consumerIdentifier, shutdownSignal);
				if (this.pendingRequests.peekMaybe (consumerIdentifier + "//consume") != null)
					this.pendingRequests.cancel (consumerIdentifier + "//consume");
				if (this.pendingRequests.peekMaybe (consumerIdentifier + "//cancel") != null)
					this.pendingRequests.cancel (consumerIdentifier + "//cancel");
			}
				break;
			default: {
				this.transcript.traceWarning ("processing unexpected message of type `%s`; ignoring...", message.specification);
			}
				break;
		}
	}
	
	/**
	 * Returns a proxy for AMQP queuing systems.
	 * 
	 * @param configuration
	 *            the execution environment of a connector
	 * @return the proxy
	 */
	public static AmqpQueueRawConnectorProxy create (final ConnectorConfiguration configuration)
	{
		final AmqpQueueRawConnectorProxy proxy = new AmqpQueueRawConnectorProxy (configuration);
		return (proxy);
	}
	
	private final ConcurrentHashMap<String, IAmqpQueueRawConsumerCallback> pendingConsumers;
}
