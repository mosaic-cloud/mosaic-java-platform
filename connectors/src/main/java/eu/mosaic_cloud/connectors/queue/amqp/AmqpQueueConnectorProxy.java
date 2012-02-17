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

import com.google.protobuf.ByteString;
import eu.mosaic_cloud.connectors.BaseConnectorProxy;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.amqp.AmqpMessage;
import eu.mosaic_cloud.platform.interop.amqp.AmqpSession;
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
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Proxy for the driver for queuing systems implementing the AMQP protocol. This
 * is used by the {@link AmqpQueueConnector} to communicate with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpQueueConnectorProxy
		extends BaseConnectorProxy
{
	private AmqpQueueConnectorProxy (final IConfiguration config, final Channel channel)
	{
		super (config, channel);
		this.pendingConsumers = new ConcurrentHashMap<String, IAmqpQueueConsumerCallbacks> ();
	}
	
	public CallbackCompletion<Boolean> ack (final long delivery, final boolean multiple)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.Ack.Builder requestBuilder = AmqpPayloads.Ack.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setDelivery (delivery);
		requestBuilder.setMultiple (multiple);
		final Message message = new Message (AmqpMessage.ACK, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> bindQueue (final String exchange, final String queue, final String routingKey)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.BindQueueRequest.Builder requestBuilder = AmqpPayloads.BindQueueRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (exchange);
		requestBuilder.setQueue (queue);
		requestBuilder.setRoutingKey (routingKey);
		final Message message = new Message (AmqpMessage.BIND_QUEUE_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> cancel (final String consumer)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.CancelRequest.Builder requestBuilder = AmqpPayloads.CancelRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setConsumer (consumer);
		final Message message = new Message (AmqpMessage.CANCEL_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> consume (final String queue, final String consumer, final boolean exclusive, final boolean autoAck, final Object extra, final IAmqpQueueConsumerCallbacks consumerCallback)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.ConsumeRequest.Builder requestBuilder = AmqpPayloads.ConsumeRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setConsumer (consumer);
		requestBuilder.setExclusive (exclusive);
		requestBuilder.setAutoAck (autoAck);
		{
			byte[] extraBytes;
			try {
				extraBytes = new PojoDataEncoder<Object> (Object.class).encode (extra);
			} catch (final Throwable exception) {
				return (CallbackCompletion.createFailure (exception));
			}
			requestBuilder.setExtra (ByteString.copyFrom (extraBytes));
		}
		this.pendingConsumers.put (consumer, consumerCallback);
		final Message mssg = new Message (AmqpMessage.CONSUME_REQUEST, requestBuilder.build ());
		return this.sendRequest (mssg, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> declareExchange (final String name, final AmqpExchangeType type, final boolean durable, final boolean autoDelete, final boolean passive)
	{
		final ExchangeType eType = AmqpPayloads.DeclareExchangeRequest.ExchangeType.valueOf (type.toString ().toUpperCase ());
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.DeclareExchangeRequest.Builder requestBuilder = AmqpPayloads.DeclareExchangeRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (name);
		requestBuilder.setType (eType);
		requestBuilder.setDurable (durable);
		requestBuilder.setAutoDelete (autoDelete);
		requestBuilder.setPassive (passive);
		final Message message = new Message (AmqpMessage.DECL_EXCHANGE_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> declareQueue (final String queue, final boolean exclusive, final boolean durable, final boolean autoDelete, final boolean passive)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.DeclareQueueRequest.Builder requestBuilder = AmqpPayloads.DeclareQueueRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setExclusive (exclusive);
		requestBuilder.setDurable (durable);
		requestBuilder.setAutoDelete (autoDelete);
		requestBuilder.setPassive (passive);
		final Message message = new Message (AmqpMessage.DECL_QUEUE_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> get (final String queue, final boolean autoAck)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.GetRequest.Builder requestBuilder = AmqpPayloads.GetRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setQueue (queue);
		requestBuilder.setAutoAck (autoAck);
		final Message message = new Message (AmqpMessage.GET_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Boolean.class);
	}
	
	public CallbackCompletion<Boolean> publish (final AmqpOutboundMessage message)
	{
		final CompletionToken token = this.generateToken ();
		final AmqpPayloads.PublishRequest.Builder requestBuilder = AmqpPayloads.PublishRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setExchange (message.getExchange ());
		requestBuilder.setRoutingKey (message.getRoutingKey ());
		requestBuilder.setData (ByteString.copyFrom (message.getData ()));
		requestBuilder.setMandatory (message.isMandatory ());
		requestBuilder.setImmediate (message.isImmediate ());
		requestBuilder.setDurable (message.isDurable ());
		if (message.getContentType () != null)
			requestBuilder.setContentType (message.getContentType ());
		if (message.getCorrelation () != null)
			requestBuilder.setCorrelationId (message.getCorrelation ());
		if (message.getCallback () != null)
			requestBuilder.setReplyTo (message.getCallback ());
		final Message mssg = new Message (AmqpMessage.PUBLISH_REQUEST, requestBuilder.build ());
		return this.sendRequest (mssg, token, Boolean.class);
	}
	
	@Override
	protected void processResponse (final Message message)
	{
		final AmqpMessage amqpMessage = (AmqpMessage) message.specification;
		final String mssgPrefix = "AmqpConnectorReactor - Received response ";
		switch (amqpMessage) {
			case OK : {
				final IdlCommon.Ok okPayload = (Ok) message.payload;
				final CompletionToken token = okPayload.getToken ();
				this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				this.pendingRequests.succeed (token.getMessageId (), Boolean.TRUE);
			}
				break;
			case NOK : {
				final IdlCommon.NotOk nokPayload = (NotOk) message.payload;
				final CompletionToken token = nokPayload.getToken ();
				this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				this.pendingRequests.succeed (token.getMessageId (), Boolean.FALSE);
			}
				break;
			case ERROR : {
				final IdlCommon.Error errorPayload = (Error) message.payload;
				final CompletionToken token = errorPayload.getToken ();
				this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				this.pendingRequests.fail (token.getMessageId (), new Exception (errorPayload.getErrorMessage ()));
			}
				break;
			case CONSUME_REPLY : {
				final AmqpPayloads.ConsumeReply consumePayload = (ConsumeReply) message.payload;
				final CompletionToken token = consumePayload.getToken ();
				this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				this.pendingRequests.succeed (token.getMessageId (), Boolean.TRUE);
			}
				break;
			case CANCEL_OK : {
				final AmqpPayloads.CancelOkMessage cancelOkPayload = (CancelOkMessage) message.payload;
				// !!!!
				// final CompletionToken token = cancelOkPayload.getToken();
				// this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				final String consumerId = cancelOkPayload.getConsumerTag ();
				this.logger.debug ("QueueConnectorProxy - Received CANCEL_OK " + " for consumer " + consumerId);
				final IAmqpQueueConsumerCallbacks callback = this.pendingConsumers.remove (consumerId);
				callback.handleCancelOk (consumerId);
				// !!!!
				// this.pendingRequests.succeed (token.getMessageId (), Boolean.TRUE);
			}
				break;
			case SERVER_CANCEL : {
				// !!!!
				// should not be sent
				final AmqpPayloads.ServerCancelRequest scancelPayload = (ServerCancelRequest) message.payload;
				final String consumerId = scancelPayload.getConsumerTag ();
				this.logger.debug ("QueueConnectorProxy - Received SERVER_CANCEL " + " for consumer " + consumerId);
				final IAmqpQueueConsumerCallbacks callback = this.pendingConsumers.remove (consumerId);
				callback.handleCancelOk (consumerId);
			}
				break;
			case CONSUME_OK : {
				final AmqpPayloads.ConsumeOkMessage consumeOkPayload = (ConsumeOkMessage) message.payload;
				// !!!!
				// final CompletionToken token = consumeOkPayload.getToken ();
				// this.logger.debug ("QueueConnectorProxy - Received " + message.specification.toString () + " request [" + token.getMessageId () + "]...");
				final String consumerId = consumeOkPayload.getConsumerTag ();
				this.logger.debug ("QueueConnectorProxy - Received CONSUME_OK " + " for consumer " + consumerId);
				final IAmqpQueueConsumerCallbacks callback = this.pendingConsumers.get (consumerId);
				callback.handleConsumeOk (consumerId);
				// !!!!
				// this.pendingRequests.succeed (token.getMessageId (), Boolean.TRUE);
			}
				break;
			case DELIVERY : {
				final AmqpPayloads.DeliveryMessage delivery = (DeliveryMessage) message.payload;
				final String consumerId = delivery.getConsumerTag ();
				this.logger.debug ("QueueConnectorProxy - Received DELIVERY " + " for consumer " + consumerId);
				final long deliveryTag = delivery.getDeliveryTag ();
				final String exchange = delivery.getExchange ();
				final String routingKey = delivery.getRoutingKey ();
				final int deliveryMode = delivery.getDeliveryMode ();
				final byte[] data = delivery.getData ().toByteArray ();
				String correlationId = null;
				String replyTo = null;
				if (delivery.hasCorrelationId ()) {
					correlationId = delivery.getCorrelationId ();
				}
				if (delivery.hasReplyTo ()) {
					replyTo = delivery.getReplyTo ();
				}
				final AmqpInboundMessage mssg = new AmqpInboundMessage (consumerId, deliveryTag, exchange, routingKey, data, deliveryMode == 2, replyTo, null, delivery.getContentType (), correlationId, null);
				final IAmqpQueueConsumerCallbacks callback = this.pendingConsumers.get (consumerId);
				callback.handleDelivery (mssg);
			}
				break;
			case SHUTDOWN : {
				final AmqpPayloads.ShutdownMessage downPayload = (ShutdownMessage) message.payload;
				final String consumerId = downPayload.getConsumerTag ();
				this.logger.debug ("QueueConnectorProxy - Received SHUTDOWN " + " for consumer " + consumerId);
				final String signalMssg = downPayload.getMessage ();
				final IAmqpQueueConsumerCallbacks callback = this.pendingConsumers.remove (consumerId);
				callback.handleShutdownSignal (consumerId, signalMssg);
			}
				break;
			default:
				break;
		}
	}
	
	private final ConcurrentHashMap<String, IAmqpQueueConsumerCallbacks> pendingConsumers;
	
	/**
	 * Returns a proxy for AMQP queuing systems.
	 * 
	 * @param configuration
	 *            the configurations required to initialize the proxy
	 * @param driverIdentity
	 *            the identifier of the driver to which request will be sent
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @return the proxy
	 */
	public static AmqpQueueConnectorProxy create (final IConfiguration configuration, final String driverIdentity, final Channel channel)
	{
		final AmqpQueueConnectorProxy proxy = new AmqpQueueConnectorProxy (configuration, channel);
		proxy.connect (driverIdentity, AmqpSession.CONNECTOR, new Message (AmqpMessage.ACCESS, null));
		return proxy;
	}
}
