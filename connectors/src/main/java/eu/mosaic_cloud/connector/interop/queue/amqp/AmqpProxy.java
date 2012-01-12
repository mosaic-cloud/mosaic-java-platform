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
package eu.mosaic_cloud.connector.interop.queue.amqp;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


import com.google.protobuf.ByteString;

import eu.mosaic_cloud.connector.interop.ConnectorProxy;
import eu.mosaic_cloud.connector.queue.amqp.AmqpConnector;
import eu.mosaic_cloud.connector.queue.amqp.IAmqpConsumerCallback;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.exceptions.ConnectionException;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.utils.SerDesUtils;
import eu.mosaic_cloud.driver.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.driver.queue.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.interop.amqp.AmqpMessage;
import eu.mosaic_cloud.interop.amqp.AmqpSession;
import eu.mosaic_cloud.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.interop.idl.amqp.AmqpPayloads;
import eu.mosaic_cloud.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest.ExchangeType;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Proxy for the driver for queuing systems implementing the AMQP protocol. This
 * is used by the {@link AmqpConnector} to communicate with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpProxy extends ConnectorProxy {

	/**
	 * Creates a proxy for AMQP queuing systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorId
	 *            the identifier of this connector's proxy
	 * @param reactor
	 *            the response reactor
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @throws Throwable
	 */
	private AmqpProxy(IConfiguration config, String connectorId,
			AmqpConnectorReactor reactor, ZeroMqChannel channel)
			throws Throwable {
		super(config, connectorId, reactor, channel);
	}

	/**
	 * Returns a proxy for AMQP queuing systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorIdentifier
	 *            the identifier of this connector
	 * @param driverIdentifier
	 *            the identifier of the driver to which request will be sent
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @return the proxy
	 * @throws Throwable
	 */
	public static AmqpProxy create(IConfiguration config,
			String connectorIdentifier, String driverIdentifier,
			ZeroMqChannel channel) throws Throwable {
		String connectorId = connectorIdentifier;

		AmqpConnectorReactor reactor = new AmqpConnectorReactor();
		AmqpProxy proxy = new AmqpProxy(config, connectorId, reactor, channel);

		proxy.connect(driverIdentifier, AmqpSession.CONNECTOR, new Message(
				AmqpMessage.ACCESS, null));
		return proxy;
	}

	public void declareExchange(String name, AmqpExchangeType type,
			boolean durable, boolean autoDelete, boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		ExchangeType eType = AmqpPayloads.DeclareExchangeRequest.ExchangeType
				.valueOf(type.toString().toUpperCase());
		CompletionToken token = generateToken();

		AmqpPayloads.DeclareExchangeRequest.Builder requestBuilder = AmqpPayloads.DeclareExchangeRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setExchange(name);
		requestBuilder.setType(eType);
		requestBuilder.setDurable(durable);
		requestBuilder.setAutoDelete(autoDelete);
		requestBuilder.setPassive(passive);

		Message message = new Message(AmqpMessage.DECL_EXCHANGE_REQUEST,
				requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	public void declareQueue(String queue, boolean exclusive, boolean durable,
			boolean autoDelete, boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.DeclareQueueRequest.Builder requestBuilder = AmqpPayloads.DeclareQueueRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setQueue(queue);
		requestBuilder.setExclusive(exclusive);
		requestBuilder.setDurable(durable);
		requestBuilder.setAutoDelete(autoDelete);
		requestBuilder.setPassive(passive);

		Message message = new Message(AmqpMessage.DECL_QUEUE_REQUEST,
				requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	public void bindQueue(String exchange, String queue, String routingKey,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.BindQueueRequest.Builder requestBuilder = AmqpPayloads.BindQueueRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setQueue(queue);
		requestBuilder.setExchange(exchange);
		requestBuilder.setRoutingKey(routingKey);

		Message message = new Message(AmqpMessage.BIND_QUEUE_REQUEST,
				requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	public void publish(AmqpOutboundMessage message,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.PublishRequest.Builder requestBuilder = AmqpPayloads.PublishRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setExchange(message.getExchange());
		requestBuilder.setData(ByteString.copyFrom(message.getData()));
		requestBuilder.setDurable(message.isDurable());
		requestBuilder.setImmediate(message.isImmediate());
		requestBuilder.setMandatory(message.isMandatory());
		requestBuilder.setRoutingKey(message.getRoutingKey());
		if (message.getContentType() != null)
			requestBuilder.setContentType(message.getContentType());
		if (message.getCorrelation() != null)
			requestBuilder.setCorrelationId(message.getCorrelation());
		if (message.getCallback() != null)
			requestBuilder.setReplyTo(message.getCallback());

		Message mssg = new Message(AmqpMessage.PUBLISH_REQUEST,
				requestBuilder.build());

		sendMessage(mssg, token, handlers);
	}

	public void consume(String queue, String consumer, boolean exclusive,
			boolean autoAck, Object extra,
			List<IOperationCompletionHandler<String>> handlers,
			IAmqpConsumerCallback consumerCallback) {

		CompletionToken token = generateToken();

		AmqpPayloads.ConsumeRequest.Builder requestBuilder = AmqpPayloads.ConsumeRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setQueue(queue);
		requestBuilder.setExclusive(exclusive);
		requestBuilder.setAutoAck(autoAck);
		try {
			requestBuilder.setExtra(ByteString.copyFrom(SerDesUtils
					.pojoToBytes(extra)));

			AmqpConnectorReactor reactor = super
					.getResponseReactor(AmqpConnectorReactor.class);
			requestBuilder.setConsumer(getConnectorId());
			reactor.addCallback(getConnectorId(), consumerCallback);

			Message mssg = new Message(AmqpMessage.CONSUME_REQUEST,
					requestBuilder.build());

			sendMessage(mssg, token, handlers);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
			ConnectionException e1 = new ConnectionException("Cannot send consume request to driver: " + e.getMessage(), e);
			for (IOperationCompletionHandler<String> handler : handlers) {
				handler.onFailure(e1);
			}
		}
	}

	public void cancel(String consumer,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.CancelRequest.Builder requestBuilder = AmqpPayloads.CancelRequest
				.newBuilder();
		requestBuilder.setToken(token);
		if ("".equals(consumer)) {
			requestBuilder.setConsumer(consumer);
		} else {
			requestBuilder.setConsumer(getConnectorId());
		}

		Message message = new Message(AmqpMessage.CANCEL_REQUEST,
				requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	public void get(String queue, boolean autoAck,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.GetRequest.Builder requestBuilder = AmqpPayloads.GetRequest
				.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setQueue(queue);
		requestBuilder.setAutoAck(autoAck);

		Message message = new Message(AmqpMessage.GET_REQUEST,
				requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	public void ack(long delivery, boolean multiple,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CompletionToken token = generateToken();

		AmqpPayloads.Ack.Builder requestBuilder = AmqpPayloads.Ack.newBuilder();
		requestBuilder.setToken(token);
		requestBuilder.setDelivery(delivery);
		requestBuilder.setMultiple(multiple);

		Message message = new Message(AmqpMessage.ACK, requestBuilder.build());

		sendMessage(message, token, handlers);
	}

	private <V extends Object> void sendMessage(Message message,
			CompletionToken token, List<IOperationCompletionHandler<V>> handlers) {
		try {
//			synchronized (this) {
				// store token and completion handlers
				super.registerHandlers(token.getMessageId(), handlers);
				super.sendRequest(
						getResponseReactor(AmqpConnectorReactor.class)
								.getSession(), message);
//			}
			MosaicLogger.getLogger().trace(
					"AmqpProxy - Sent " + message.specification.toString()
							+ " request [" + token.getMessageId() + "]...");
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
			ConnectionException e1 = new ConnectionException("Cannot send " + message.specification.toString() + " request to driver: " + e.getMessage(), e);
			for (IOperationCompletionHandler<V> handler : handlers) {
				handler.onFailure(e1);
			}
		}
	}

	private CompletionToken generateToken() {
		String identifier = UUID.randomUUID().toString();
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(identifier);
		tokenBuilder.setClientId(getConnectorId());
		return tokenBuilder.build();
	}

}
