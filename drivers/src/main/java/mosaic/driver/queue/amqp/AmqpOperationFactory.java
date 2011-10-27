/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.driver.queue.amqp;

import java.io.IOException;
import java.util.concurrent.Callable;

import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * for the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
final class AmqpOperationFactory implements IOperationFactory { // NOPMD by georgiana on 10/12/11 4:13 PM

	/**
	 * 
	 */
	private final AmqpDriver amqpDriver;

	AmqpOperationFactory(AmqpDriver amqpDriver) {
		super();
		this.amqpDriver = amqpDriver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.core.IOperationFactory#getOperation(mosaic.core.IOperationType ,
	 * java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation(final IOperationType type, // NOPMD by georgiana on 10/12/11 4:13 PM
			Object... parameters) {
		IOperation<?> operation;
		if (!(type instanceof AmqpOperations)) {
			return new GenericOperation<Object>(new Callable<Object>() { // NOPMD by georgiana on 10/12/11 4:13 PM

						@Override
						public Object call()
								throws UnsupportedOperationException {
							throw new UnsupportedOperationException(
									"Unsupported operation: " + type.toString());
						}

					});
		}

		final AmqpOperations mType = (AmqpOperations) type;

		switch (mType) {
		case DECLARE_EXCHANGE:
			operation = buildDeclareExchangeOperation(parameters);
			break;
		case DECLARE_QUEUE:
			operation = buildDeclareQueueOperation(parameters);
			break;
		case BIND_QUEUE:
			operation = buildBindQueueOperation(parameters);
			break;
		case PUBLISH:
			operation = buildPublishOperation(parameters);
			break;
		case CONSUME:
			operation = buildConsumeOperation(parameters);
			break;
		case GET:
			operation = buildGetOperation(parameters);
			break;
		case ACK:
			operation = buildAckOperation(parameters);
			break;
		case CANCEL:
			operation = buildCancelOperation(parameters);
			break;
		default:
			operation = new GenericOperation<Object>(new Callable<Object>() { // NOPMD by georgiana on 10/12/11 4:14 PM

						@Override
						public Object call()
								throws UnsupportedOperationException {
							throw new UnsupportedOperationException(
									"Unsupported operation: "
											+ mType.toString());
						}

					});
		}

		return operation;
	}

	private IOperation<?> buildCancelOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				boolean succeeded = false;
				String consumer = (String) parameters[0];

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					final Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(consumer);
					if (channel != null) {
						try {
							channel.basicCancel(consumer);
							succeeded = true;
						} catch (IOException e) {
							ExceptionTracer.traceDeferred(e);
						}
					}
				}
				return succeeded;
			}

		});
	}

	private IOperation<?> buildAckOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				boolean succeeded = false;
				long delivery = (Long) parameters[0];
				boolean multiple = (Boolean) parameters[1];
				String consumer = (String) parameters[2];

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					final Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(consumer);
					if (channel != null) {
						try {
							channel.basicAck(delivery, multiple);
							succeeded = true;
						} catch (IOException e) {
							ExceptionTracer.traceDeferred(e);
						}
					}
				}
				return succeeded;
			}

		});
	}

	private IOperation<?> buildGetOperation(final Object... parameters) {
		return new GenericOperation<AmqpInboundMessage>(
				new Callable<AmqpInboundMessage>() {

					@Override
					public AmqpInboundMessage call() {
						AmqpInboundMessage message = null;
						String queue = (String) parameters[0];
						boolean autoAck = (Boolean) parameters[1];
						String clientId = (String) parameters[2];

						synchronized (AmqpOperationFactory.this.amqpDriver) {

							final Channel channel = AmqpOperationFactory.this.amqpDriver
									.getChannel(clientId);
							if (channel != null) {
								GetResponse outcome = null;
								try {
									outcome = channel.basicGet(queue, autoAck);
									if (outcome != null) {
										final Envelope envelope = outcome
												.getEnvelope();
										final AMQP.BasicProperties properties = outcome
												.getProps();
										message = new AmqpInboundMessage(
												null,
												envelope.getDeliveryTag(),
												envelope.getExchange(),
												envelope.getRoutingKey(),
												outcome.getBody(),
												properties.getDeliveryMode() == 2 ? true
														: false,
												properties.getReplyTo(),
												properties.getContentEncoding(),
												properties.getContentType(),
												properties.getCorrelationId(),
												properties.getMessageId());
									}
								} catch (IOException e) {
									ExceptionTracer.traceDeferred(e);
								}
							}
						}
						return message;
					}

				});
	}

	private IOperation<?> buildConsumeOperation(final Object... parameters) {
		return new GenericOperation<String>(new Callable<String>() {

			@Override
			public String call() throws IOException {
				String queue = (String) parameters[0];
				String consumer = (String) parameters[1];
				boolean exclusive = (Boolean) parameters[2];
				boolean autoAck = (Boolean) parameters[3];
				Object extra = parameters[4];
				IAmqpConsumer consumeCallback = (IAmqpConsumer) parameters[5];
				String consumerTag;

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(consumer);
					if (channel != null) {
						AmqpOperationFactory.this.amqpDriver.consumers.put(
								consumer, consumeCallback);
						consumerTag = channel
								.basicConsume(
										queue,
										autoAck,
										consumer,
										true,
										exclusive,
										null,
										AmqpOperationFactory.this.amqpDriver.new ConsumerCallback(
												extra));
						if (!consumer.equals(consumerTag))
							MosaicLogger.getLogger().error(
									"Received different consumer tag: consumerTag = "
											+ consumerTag + " consumer "
											+ consumer);
					}
				}

				return consumer;
			}

		});
	}

	private IOperation<?> buildPublishOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() throws IOException {
				boolean succeeded = false;
				AmqpOutboundMessage message = (AmqpOutboundMessage) parameters[0];
				String clientId = (String) parameters[1];

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(clientId);

					if (channel != null) {
						AMQP.BasicProperties properties = new AMQP.BasicProperties(
								message.getContentType(), message
										.getContentEncoding(), null, message
										.isDurable() ? 2 : 1, 0, message
										.getCorrelation(), message
										.getCallback(), null, message
										.getIdentifier(), null, null, null,
								null, null);
						channel.basicPublish(message.getExchange(),
								message.getRoutingKey(), properties,
								message.getData());
						succeeded = true;
					}
				}
				return succeeded;
			}

		});
	}

	private IOperation<?> buildBindQueueOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				boolean succeeded = false;
				String exchange = (String) parameters[0];
				String queue = (String) parameters[1];
				String routingKey = (String) parameters[2];
				String clientId = (String) parameters[3];

				synchronized (AmqpOperationFactory.this.amqpDriver) {

					try {
						Channel channel = AmqpOperationFactory.this.amqpDriver
								.getChannel(clientId);
						if (channel != null) {
							AMQP.Queue.BindOk outcome = channel.queueBind(
									queue, exchange, routingKey, null);
							succeeded = (outcome != null);
						}
					} catch (IOException e) {
						ExceptionTracer.traceDeferred(e);
					}
				}
				return succeeded;
			}

		});
	}

	private IOperation<?> buildDeclareQueueOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() throws IOException {
				boolean succeeded = false;
				String queue = (String) parameters[0];
				boolean exclusive = (Boolean) parameters[1];
				boolean durable = (Boolean) parameters[2];
				boolean autoDelete = (Boolean) parameters[3];
				boolean passive = (Boolean) parameters[4];
				String clientId = (String) parameters[5];

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(clientId);
					if (channel != null) {
						AMQP.Queue.DeclareOk outcome = null;
						if (passive) {
							outcome = channel.queueDeclarePassive(queue);
						} else {
							outcome = channel.queueDeclare(queue, durable,
									exclusive, autoDelete, null);
						}
						succeeded = (outcome != null);
					}
				}
				return succeeded;
			}

		});
	}

	private IOperation<?> buildDeclareExchangeOperation(
			final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() throws IOException {
				boolean succeeded = false;
				String exchange = (String) parameters[0];
				boolean durable = (Boolean) parameters[2];
				boolean autoDelete = (Boolean) parameters[3];
				boolean passive = (Boolean) parameters[4];
				AmqpExchangeType eType = (AmqpExchangeType) parameters[1];
				String clientId = (String) parameters[5];

				synchronized (AmqpOperationFactory.this.amqpDriver) {
					Channel channel = AmqpOperationFactory.this.amqpDriver
							.getChannel(clientId);
					if (channel != null) {
						AMQP.Exchange.DeclareOk outcome = null;
						if (passive) {
							outcome = channel.exchangeDeclarePassive(exchange);
						} else {
							outcome = channel.exchangeDeclare(exchange,
									eType.getAmqpName(), durable, autoDelete,
									null);
						}
						succeeded = (outcome != null);
					}
				}
				return succeeded;
			}

		});
	}

	@Override
	public void destroy() {
		// nothing to do here
	}
}