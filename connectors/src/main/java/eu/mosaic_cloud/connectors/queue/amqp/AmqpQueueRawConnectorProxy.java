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
import eu.mosaic_cloud.connectors.core.ResponseHandlerMap;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
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

import com.google.protobuf.ByteString;

/**
 * Proxy for the driver for queuing systems implementing the AMQP protocol. This
 * is used by the {@link AmqpQueueRawConnector} to communicate with a AMQP
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpQueueRawConnectorProxy extends BaseConnectorProxy implements
        IAmqpQueueRawConnector { // NOPMD

    private final ResponseHandlerMap consumerMessages;

    private final ConcurrentHashMap<String, IAmqpQueueRawConsumerCallback> pendingConsumers;

    // by
    // georgiana
    // on
    // 2/21/12
    // 2:36
    // PM
    protected AmqpQueueRawConnectorProxy(final IConfiguration config,
    		final ConnectorEnvironment environment) {
        super(config, environment);
        this.pendingConsumers = new ConcurrentHashMap<String, IAmqpQueueRawConsumerCallback>();
        this.consumerMessages = new ResponseHandlerMap();
    }

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
    public static AmqpQueueRawConnectorProxy create(final IConfiguration configuration,
    		final ConnectorEnvironment environment) {
        final AmqpQueueRawConnectorProxy proxy = new AmqpQueueRawConnectorProxy(
        		configuration, environment);
        return proxy;
    }

    @Override
    public CallbackCompletion<Void> ack(final long delivery, final boolean multiple) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.Ack.Builder requestBuilder = AmqpPayloads.Ack.newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setDelivery(delivery);
        requestBuilder.setMultiple(multiple);
        final Message message = new Message(AmqpMessage.ACK, requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> bindQueue(final String exchange, final String queue,
            final String routingKey) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.BindQueueRequest.Builder requestBuilder = AmqpPayloads.BindQueueRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setExchange(exchange);
        requestBuilder.setQueue(queue);
        requestBuilder.setRoutingKey(routingKey);
        final Message message = new Message(AmqpMessage.BIND_QUEUE_REQUEST, requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> cancel(final String consumer) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.CancelRequest.Builder requestBuilder = AmqpPayloads.CancelRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setConsumer(consumer);
        final Message message = new Message(AmqpMessage.CANCEL_REQUEST, requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> consume(final String queue, final String consumer,
            final boolean exclusive, final boolean autoAck,
            final IAmqpQueueRawConsumerCallback consumerCallback) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.ConsumeRequest.Builder requestBuilder = AmqpPayloads.ConsumeRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setQueue(queue);
        requestBuilder.setConsumer(consumer);
        requestBuilder.setExclusive(exclusive);
        requestBuilder.setAutoAck(autoAck);
        this.pendingConsumers.put(consumer, consumerCallback);
        final Message mssg = new Message(AmqpMessage.CONSUME_REQUEST, requestBuilder.build());
        final CallbackCompletion<Void> result = this.sendRequest(mssg, token, Void.class); // NOPMD
                                                                                           // by
                                                                                           // georgiana
                                                                                           // on
                                                                                           // 2/20/12
                                                                                           // 5:54
                                                                                           // PM
        final CallbackCompletionDeferredFuture<String> consumeFuture = CallbackCompletionDeferredFuture
                .create(String.class);
        this.consumerMessages.register(consumer, consumeFuture);
        return result;
    }

    @Override
    public CallbackCompletion<Void> declareExchange(final String name, final AmqpExchangeType type,
            final boolean durable, final boolean autoDelete, final boolean passive) {
        final ExchangeType eType = AmqpPayloads.DeclareExchangeRequest.ExchangeType.valueOf(type
                .toString().toUpperCase());
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.DeclareExchangeRequest.Builder requestBuilder = AmqpPayloads.DeclareExchangeRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setExchange(name);
        requestBuilder.setType(eType);
        requestBuilder.setDurable(durable);
        requestBuilder.setAutoDelete(autoDelete);
        requestBuilder.setPassive(passive);
        final Message message = new Message(AmqpMessage.DECL_EXCHANGE_REQUEST,
                requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> declareQueue(final String queue, final boolean exclusive,
            final boolean durable, final boolean autoDelete, final boolean passive) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.DeclareQueueRequest.Builder requestBuilder = AmqpPayloads.DeclareQueueRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setQueue(queue);
        requestBuilder.setExclusive(exclusive);
        requestBuilder.setDurable(durable);
        requestBuilder.setAutoDelete(autoDelete);
        requestBuilder.setPassive(passive);
        final Message message = new Message(AmqpMessage.DECL_QUEUE_REQUEST, requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> destroy() {
    	return this.disconnect(null);
    }

    @Override
    public CallbackCompletion<Void> get(final String queue, final boolean autoAck) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.GetRequest.Builder requestBuilder = AmqpPayloads.GetRequest.newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setQueue(queue);
        requestBuilder.setAutoAck(autoAck);
        final Message message = new Message(AmqpMessage.GET_REQUEST, requestBuilder.build());
        return this.sendRequest(message, token, Void.class);
    }

    @Override
    public CallbackCompletion<Void> initialize() {
    	return this.connect(AmqpSession.CONNECTOR, new Message(AmqpMessage.ACCESS, null));
    }

    @Override
    protected void processResponse(final Message message) { // NOPMD by
                                                            // georgiana on
                                                            // 2/21/12 2:35 PM
        final AmqpMessage amqpMessage = (AmqpMessage) message.specification;
        switch (amqpMessage) {
        case OK: {
            final IdlCommon.Ok okPayload = (Ok) message.payload;
            final CompletionToken token = okPayload.getToken();
            this.logger.debug("QueueConnectorProxy - Received " + message.specification.toString()
                    + " request [" + token.getMessageId() + "]...");
            this.pendingRequests.succeed(token.getMessageId(), null);
        }
            break;
        case NOK: {
            final IdlCommon.NotOk nokPayload = (NotOk) message.payload;
            final CompletionToken token = nokPayload.getToken();
            this.logger.debug("QueueConnectorProxy - Received " + message.specification.toString()
                    + " request [" + token.getMessageId() + "]...");
            // FIXME
            this.pendingRequests.fail(token.getMessageId(), new Exception("operation failed"));
        }
            break;
        case ERROR: {
            final IdlCommon.Error errorPayload = (Error) message.payload;
            final CompletionToken token = errorPayload.getToken();
            this.logger.debug("QueueConnectorProxy - Received " + message.specification.toString()
                    + " request [" + token.getMessageId() + "]...");
            this.pendingRequests.fail(token.getMessageId(),
                    new Exception(errorPayload.getErrorMessage())); // NOPMD by
                                                                    // georgiana
                                                                    // on
                                                                    // 2/20/12
                                                                    // 5:53 PM
        }
            break;
        case CONSUME_REPLY: {
            final AmqpPayloads.ConsumeReply consumePayload = (ConsumeReply) message.payload;
            final CompletionToken token = consumePayload.getToken();
            this.logger.debug("QueueConnectorProxy - Received " + message.specification.toString()
                    + " request [" + token.getMessageId() + "]...");
            this.pendingRequests.succeed(token.getMessageId(), null);
        }
            break;
        case CANCEL_OK: {
            final AmqpPayloads.CancelOkMessage cancelOkPayload = (CancelOkMessage) message.payload;
            final String consumerId = cancelOkPayload.getConsumerTag();
            this.logger.debug("QueueConnectorProxy - Received CANCEL_OK " + " for consumer "
                    + consumerId);
            final IAmqpQueueRawConsumerCallback callback = this.pendingConsumers.remove(consumerId);
            callback.handleCancelOk(consumerId);
            this.consumerMessages.succeed(consumerId, null);
            this.consumerMessages.cancel(consumerId);
        }
            break;
        case SERVER_CANCEL: {
            // FIXME
            final AmqpPayloads.ServerCancelRequest scancelPayload = (ServerCancelRequest) message.payload;
            final String consumerId = scancelPayload.getConsumerTag();
            this.logger.debug("QueueConnectorProxy - Received SERVER_CANCEL " + " for consumer "
                    + consumerId);
            this.pendingConsumers.remove(consumerId);
            // callback.handleCancelOk(consumerId);
            this.consumerMessages.cancel(consumerId);
        }
            break;
        case CONSUME_OK: {
            final AmqpPayloads.ConsumeOkMessage consumeOkPayload = (ConsumeOkMessage) message.payload;
            final String consumerId = consumeOkPayload.getConsumerTag();
            this.logger.debug("QueueConnectorProxy - Received CONSUME_OK " + " for consumer "
                    + consumerId);
            final IAmqpQueueRawConsumerCallback callback = this.pendingConsumers.get(consumerId);
            callback.handleConsumeOk(consumerId);
            this.consumerMessages.succeed(consumerId, null);
        }
            break;
        case DELIVERY: {
            final AmqpPayloads.DeliveryMessage delivery = (DeliveryMessage) message.payload;
            final String consumerId = delivery.getConsumerTag();
            this.logger.debug("QueueConnectorProxy - Received DELIVERY " + " for consumer "
                    + consumerId);
            final long deliveryTag = delivery.getDeliveryTag();
            final String exchange = delivery.getExchange();
            final String routingKey = delivery.getRoutingKey();
            final int deliveryMode = delivery.getDeliveryMode();
            final byte[] data = delivery.getData().toByteArray();
            String correlationId = null; // NOPMD by georgiana on 2/20/12 5:55
                                         // PM
            String replyTo = null; // NOPMD by georgiana on 2/20/12 5:55 PM
            if (delivery.hasCorrelationId()) {
                correlationId = delivery.getCorrelationId();
            }
            if (delivery.hasReplyTo()) {
                replyTo = delivery.getReplyTo();
            }
            final AmqpInboundMessage mssg = new AmqpInboundMessage(consumerId, deliveryTag,
                    exchange, routingKey, data, deliveryMode == 2, replyTo, null,
                    delivery.getContentType(), correlationId, null);
            final IAmqpQueueRawConsumerCallback callback = this.pendingConsumers.get(consumerId);
            callback.handleDelivery(mssg);
        }
            break;
        case SHUTDOWN: {
            final AmqpPayloads.ShutdownMessage downPayload = (ShutdownMessage) message.payload;
            final String consumerId = downPayload.getConsumerTag();
            this.logger.debug("QueueConnectorProxy - Received SHUTDOWN " + " for consumer "
                    + consumerId);
            final String signalMssg = downPayload.getMessage();
            final IAmqpQueueRawConsumerCallback callback = this.pendingConsumers.remove(consumerId);
            callback.handleShutdownSignal(consumerId, signalMssg);
        }
            break;
        default:
            break;
        }
    }

    @Override
    public CallbackCompletion<Void> publish(final AmqpOutboundMessage message) {
        final CompletionToken token = this.generateToken();
        final AmqpPayloads.PublishRequest.Builder requestBuilder = AmqpPayloads.PublishRequest
                .newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setExchange(message.getExchange());
        requestBuilder.setRoutingKey(message.getRoutingKey());
        requestBuilder.setData(ByteString.copyFrom(message.getData()));
        requestBuilder.setMandatory(message.isMandatory());
        requestBuilder.setImmediate(message.isImmediate());
        requestBuilder.setDurable(message.isDurable());
        if (message.getContentType() != null) {
            requestBuilder.setContentType(message.getContentType());
        }
        if (message.getCorrelation() != null) {
            requestBuilder.setCorrelationId(message.getCorrelation());
        }
        if (message.getCallback() != null) {
            requestBuilder.setReplyTo(message.getCallback());
        }
        final Message mssg = new Message(AmqpMessage.PUBLISH_REQUEST, requestBuilder.build());
        return this.sendRequest(mssg, token, Void.class);
    }
}
