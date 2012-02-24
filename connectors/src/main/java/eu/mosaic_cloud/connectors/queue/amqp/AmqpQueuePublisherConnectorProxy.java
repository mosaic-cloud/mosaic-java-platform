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
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Message>
 *            the type of the messages published by the cloudlet
 */
public class AmqpQueuePublisherConnectorProxy<Message>
		extends AmqpQueueConnectorProxy<Message>
{
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
	public AmqpQueuePublisherConnectorProxy (final AmqpQueueRawConnectorProxy raw, final IConfiguration config, final Class<Message> dataClass, final DataEncoder<Message> encoder)
	{
		super (raw, config, dataClass, encoder);
		this.identity = UUID.randomUUID ().toString ();
		this.exchange = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.0"), String.class, this.identity); //$NON-NLS-1$ 
		this.exchangeType = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.5"), AmqpExchangeType.class, AmqpExchangeType.DIRECT);//$NON-NLS-1$
		this.exchangeDurable = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.9"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
		this.exchangeAutoDelete = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.7"), Boolean.class, Boolean.TRUE).booleanValue (); //$NON-NLS-1$
		this.publishRoutingKey = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.1"), String.class, this.identity); //$NON-NLS-1$ 
		this.definePassive = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("AmqpQueueConnector.8"), Boolean.class, Boolean.FALSE).booleanValue (); //$NON-NLS-1$ 
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		return this.raw.destroy ();
	}
	
	public CallbackCompletion<Void> initialize ()
	{
		// !!!!
		return this.raw.declareExchange (this.exchange, this.exchangeType, this.exchangeDurable, this.exchangeAutoDelete, this.definePassive);
	}
	
	public CallbackCompletion<Void> publish (final Message message)
	{
		final byte[] data;
		try {
			data = this.messageEncoder.encode (message);
		} catch (final EncodingException exception) {
			ExceptionTracer.traceDeferred (exception);
			return (CallbackCompletion.createFailure (exception));
		}
		final AmqpOutboundMessage outbound = new AmqpOutboundMessage (this.exchange, this.publishRoutingKey, data, false, false, false, null);
		return this.raw.publish (outbound);
	}
	
	protected final boolean definePassive;
	protected final String exchange;
	protected final boolean exchangeAutoDelete;
	protected final boolean exchangeDurable;
	protected final AmqpExchangeType exchangeType;
	protected final String identity;
	protected final String publishRoutingKey;
}
