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


import java.util.UUID;

import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Message>
 *            the type of the messages consumed by the cloudlet
 */
public class AmqpQueueConsumerConnectorProxy<Message>
		extends AmqpQueueConnectorProxy<Message>
{
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
	public AmqpQueueConsumerConnectorProxy (final AmqpQueueRawConnectorProxy raw, final IConfiguration config, final Class<Message> dataClass, final DataEncoder<Message> encoder, final IAmqpQueueConsumerCallback<Message> callback)
	{
		super (raw, config, dataClass, encoder);
		this.identity = UUID.randomUUID ().toString ();
		this.exchange = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.0"), String.class, this.identity); //$NON-NLS-1$ 
		this.exchangeType = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.5"), AmqpExchangeType.class, AmqpExchangeType.DIRECT);//$NON-NLS-1$
		this.exchangeDurable = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.9"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
		this.exchangeAutoDelete = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.7"), Boolean.class, Boolean.TRUE).booleanValue (); //$NON-NLS-1$
		this.queue = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.2"), String.class, this.identity); //$NON-NLS-1$ 
		this.queueExclusive = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.6"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
		this.queueAutoDelete = this.exchangeAutoDelete;
		this.queueDurable = this.exchangeDurable;
		this.bindingRoutingKey = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.1"), String.class, this.identity); //$NON-NLS-1$ 
		this.consumerAutoAck = ConfigUtils.resolveParameter (this.config, ConfigProperties.getString ("AmqpQueueConnector.10"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
		this.definePassive = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.8"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
		this.callback = new AmqpConsumerCallback (callback);
	}
	
	public CallbackCompletion<Void> acknowledge (final IAmqpQueueDeliveryToken delivery)
	{
		this.raw.ack (((DeliveryToken) delivery).token, false);
		// !!!!
		return null;
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		this.raw.cancel (this.identity);
		this.raw.destroy ();
		// !!!!
		return null;
	}
	
	public CallbackCompletion<Void> initialize ()
	{
		this.raw.declareExchange (this.exchange, this.exchangeType, this.exchangeDurable, this.exchangeAutoDelete, this.definePassive);
		this.raw.declareQueue (this.queue, this.queueExclusive, this.queueDurable, this.queueAutoDelete, this.definePassive);
		this.raw.bindQueue (this.exchange, this.queue, this.bindingRoutingKey);
		this.raw.consume (this.queue, this.identity, this.queueExclusive, this.consumerAutoAck, this.callback);
		// !!!!
		return null;
	}
	
	protected final String bindingRoutingKey;
	protected final AmqpConsumerCallback callback;
	protected final String identity;
	protected final boolean consumerAutoAck;
	protected final boolean definePassive;
	protected final String exchange;
	protected final boolean exchangeAutoDelete;
	protected final boolean exchangeDurable;
	protected final AmqpExchangeType exchangeType;
	protected final String queue;
	protected final boolean queueAutoDelete;
	protected final boolean queueDurable;
	protected final boolean queueExclusive;
	
	protected class AmqpConsumerCallback
			extends Object
			implements
				IAmqpQueueRawConsumerCallback
	{
		protected AmqpConsumerCallback (final IAmqpQueueConsumerCallback<Message> delegate)
		{
			super ();
			this.delegate = delegate;
		}
		
		@Override
		public CallbackCompletion<Void> handleCancelOk (final String consumerTag)
		{
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleConsumeOk (final String consumerTag)
		{
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleDelivery (final AmqpInboundMessage inbound)
		{
			final DeliveryToken delivery = new DeliveryToken (inbound.getDelivery ());
			final Message message;
			try {
				message = AmqpQueueConsumerConnectorProxy.this.messageEncoder.decode (inbound.getData ());
			} catch (final EncodingException exception) {
				ExceptionTracer.traceDeferred (exception);
				return (CallbackCompletion.createFailure (exception));
			}
			return this.delegate.consume (delivery, message);
		}
		
		@Override
		public CallbackCompletion<Void> handleShutdownSignal (final String consumerTag, final String message)
		{
			return CallbackCompletion.createOutcome ();
		}
		
		protected final IAmqpQueueConsumerCallback<Message> delegate;
	}
	
	protected static class DeliveryToken
			extends Object
			implements
				IAmqpQueueDeliveryToken
	{
		DeliveryToken (final long token)
		{
			super ();
			this.token = token;
		}
		
		public final long token;
	}
}
