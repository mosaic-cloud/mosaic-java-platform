/*
 * #%L
 * mosaic-drivers-stubs-amqp
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

package eu.mosaic_cloud.drivers.queue.amqp;


import java.io.IOException;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;

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
final class AmqpOperationFactory
		implements
			IOperationFactory
{ // NOPMD
	AmqpOperationFactory (final AmqpDriver amqpDriver)
	{
		super ();
		this.amqpDriver = amqpDriver;
		this.exceptions = FallbackExceptionTracer.defaultInstance;
	}
	
	@Override
	public void destroy ()
	{
		// NOTE: nothing to do here
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.mosaic_cloud.platform.core.IOperationFactory#getOperation(eu.mosaic_cloud
	 * .platform.core.IOperationType , java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation (final IOperationType type, final Object ... parameters) // NOPMD
	{
		IOperation<?> operation;
		if (!(type instanceof AmqpOperations)) {
			return new GenericOperation<Object> (new Callable<Object> () { // NOPMD
						@Override
						public Object call ()
								throws UnsupportedOperationException
						{
							throw new UnsupportedOperationException ("Unsupported operation: " + type.toString ());
						}
					});
		}
		final AmqpOperations mType = (AmqpOperations) type;
		switch (mType) {
			case DECLARE_EXCHANGE :
				operation = this.buildDeclareExchangeOperation (parameters);
				break;
			case DECLARE_QUEUE :
				operation = this.buildDeclareQueueOperation (parameters);
				break;
			case BIND_QUEUE :
				operation = this.buildBindQueueOperation (parameters);
				break;
			case PUBLISH :
				operation = this.buildPublishOperation (parameters);
				break;
			case CONSUME :
				operation = this.buildConsumeOperation (parameters);
				break;
			case GET :
				operation = this.buildGetOperation (parameters);
				break;
			case ACK :
				operation = this.buildAckOperation (parameters);
				break;
			case CANCEL :
				operation = this.buildCancelOperation (parameters);
				break;
			default:
				operation = new GenericOperation<Object> (new Callable<Object> () { // NOPMD
							@Override
							public Object call ()
									throws UnsupportedOperationException
							{
								throw new UnsupportedOperationException ("Unsupported operation: " + mType.toString ());
							}
						});
		}
		return operation;
	}
	
	private IOperation<?> buildAckOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				boolean succeeded = false;
				final long delivery = (Long) parameters[0];
				final boolean multiple = (Boolean) parameters[1];
				final String consumer = (String) parameters[2];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (consumer);
				if (channel != null) {
					try {
						channel.basicAck (delivery, multiple);
						succeeded = true;
					} catch (final IOException e) {
						AmqpOperationFactory.this.exceptions.traceIgnoredException (e);
					}
				}
				return succeeded;
			}
		});
	}
	
	private IOperation<?> buildBindQueueOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				boolean succeeded = false;
				final String exchange = (String) parameters[0];
				final String queue = (String) parameters[1];
				final String routingKey = (String) parameters[2];
				final String clientId = (String) parameters[3];
				try {
					final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (clientId);
					if (channel != null) {
						final AMQP.Queue.BindOk outcome = channel.queueBind (queue, exchange, routingKey, null);
						succeeded = (outcome != null);
					}
				} catch (final IOException e) {
					AmqpOperationFactory.this.exceptions.traceIgnoredException (e);
				}
				return succeeded;
			}
		});
	}
	
	private IOperation<?> buildCancelOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				boolean succeeded = false;
				final String consumer = (String) parameters[0];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (consumer);
				if (channel != null) {
					try {
						channel.basicCancel (consumer);
						succeeded = true;
					} catch (final IOException e) {
						AmqpOperationFactory.this.exceptions.traceIgnoredException (e);
					}
				}
				return succeeded;
			}
		});
	}
	
	private IOperation<?> buildConsumeOperation (final Object ... parameters)
	{
		return new GenericOperation<String> (new Callable<String> () {
			@Override
			public String call ()
					throws IOException
			{
				final String queue = (String) parameters[0];
				final String consumer = (String) parameters[1];
				final boolean exclusive = (Boolean) parameters[2];
				final boolean autoAck = (Boolean) parameters[3];
				// FIXME: we should allow explicit setting of QOS
				final int qos = 1;
				final IAmqpConsumer consumeCallback = (IAmqpConsumer) parameters[4];
				String consumerTag;
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (consumer);
				if (channel != null) {
					AmqpOperationFactory.this.amqpDriver.consumers.put (consumer, consumeCallback);
					channel.basicQos (qos);
					consumerTag = channel.basicConsume (queue, autoAck, consumer, true, exclusive, null, AmqpOperationFactory.this.amqpDriver.new ConsumerCallback ());
					if (!consumer.equals (consumerTag)) {
						AmqpOperationFactory.logger.error ("Received different consumer tag: consumerTag = " + consumerTag + " consumer " + consumer);
					}
				}
				return consumer;
			}
		});
	}
	
	private IOperation<?> buildDeclareExchangeOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws IOException
			{
				boolean succeeded = false;
				final String exchange = (String) parameters[0];
				final boolean durable = (Boolean) parameters[2];
				final boolean autoDelete = (Boolean) parameters[3];
				final boolean passive = (Boolean) parameters[4];
				final AmqpExchangeType eType = (AmqpExchangeType) parameters[1];
				final String clientId = (String) parameters[5];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (clientId);
				if (channel != null) {
					AMQP.Exchange.DeclareOk outcome = null;
					if (passive) {
						outcome = channel.exchangeDeclarePassive (exchange);
					} else {
						outcome = channel.exchangeDeclare (exchange, eType.getAmqpName (), durable, autoDelete, null);
					}
					succeeded = (outcome != null);
				}
				return succeeded;
			}
		});
	}
	
	private IOperation<?> buildDeclareQueueOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws IOException
			{
				boolean succeeded = false;
				final String queue = (String) parameters[0];
				final boolean exclusive = (Boolean) parameters[1];
				final boolean durable = (Boolean) parameters[2];
				final boolean autoDelete = (Boolean) parameters[3];
				final boolean passive = (Boolean) parameters[4];
				final String clientId = (String) parameters[5];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (clientId);
				if (channel != null) {
					AMQP.Queue.DeclareOk outcome = null;
					if (passive) {
						outcome = channel.queueDeclarePassive (queue);
					} else {
						outcome = channel.queueDeclare (queue, durable, exclusive, autoDelete, null);
					}
					succeeded = (outcome != null);
				}
				return succeeded;
			}
		});
	}
	
	private IOperation<?> buildGetOperation (final Object ... parameters)
	{
		return new GenericOperation<AmqpInboundMessage> (new Callable<AmqpInboundMessage> () {
			@Override
			public AmqpInboundMessage call ()
			{
				AmqpInboundMessage message = null;
				final String queue = (String) parameters[0];
				final boolean autoAck = (Boolean) parameters[1];
				final String clientId = (String) parameters[2];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (clientId);
				if (channel != null) {
					GetResponse outcome = null;
					try {
						outcome = channel.basicGet (queue, autoAck);
						if (outcome != null) {
							final Envelope envelope = outcome.getEnvelope ();
							final AMQP.BasicProperties properties = outcome.getProps ();
							message = new AmqpInboundMessage (null, envelope.getDeliveryTag (), envelope.getExchange (), envelope.getRoutingKey (), outcome.getBody (), properties.getDeliveryMode () == 2 ? true : false, properties.getReplyTo (), properties.getContentEncoding (), properties.getContentType (), properties.getCorrelationId (), properties.getMessageId ());
						}
					} catch (final IOException e) {
						AmqpOperationFactory.this.exceptions.traceIgnoredException (e);
					}
				}
				return message;
			}
		});
	}
	
	private IOperation<?> buildPublishOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws IOException
			{
				boolean succeeded = false;
				final AmqpOutboundMessage message = (AmqpOutboundMessage) parameters[0];
				final String clientId = (String) parameters[1];
				final Channel channel = AmqpOperationFactory.this.amqpDriver.getChannel (clientId);
				if (channel != null) {
					final AMQP.BasicProperties properties = new AMQP.BasicProperties (message.getContentType (), message.getContentEncoding (), null, message.isDurable () ? 2 : 1, 0, message.getCorrelation (), message.getCallback (), null, message.getIdentifier (), null, null, null, null, null);
					channel.basicPublish (message.getExchange (), message.getRoutingKey (), properties, message.getData ());
					succeeded = true;
				}
				return succeeded;
			}
		});
	}
	
	private final AmqpDriver amqpDriver;
	private final BaseExceptionTracer exceptions;
	private static final Logger logger = Transcript.create (AmqpOperationFactory.class).adaptAs (Logger.class);
}
