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


import java.util.concurrent.Callable;

import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionWorkflows;

import com.google.common.base.Preconditions;


public final class AmqpQueuePublisherConnectorProxy<TMessage>
		extends AmqpQueueConnectorProxy<TMessage>
		implements
			IAmqpQueuePublisherConnector<TMessage>
{
	private AmqpQueuePublisherConnectorProxy (final AmqpQueueRawConnectorProxy rawProxy, final ConnectorConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder)
	{
		super (rawProxy, configuration, messageClass, messageEncoder);
		this.exchange = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.0"), String.class, this.raw.getIdentifier ()); // $NON-NLS-1$
		this.exchangeType = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.5"), AmqpExchangeType.class, AmqpExchangeType.DIRECT); // $NON-NLS-1$
		this.exchangeDurable = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.9"), Boolean.class, Boolean.FALSE).booleanValue (); // $NON-NLS-1$
		this.exchangeAutoDelete = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.7"), Boolean.class, Boolean.TRUE).booleanValue (); // $NON-NLS-1$
		this.publishRoutingKey = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.1"), String.class, this.raw.getIdentifier ()); // $NON-NLS-1$
		this.definePassive = configuration.getConfigParameter (ConfigProperties.getString ("AmqpQueueConnector.8"), Boolean.class, Boolean.FALSE).booleanValue (); // $NON-NLS-1$
		this.transcript.traceDebugging ("created the queue publisher connector proxy for exchange `%s` (of type `%s`) with routing key `%s`.", this.exchange, this.exchangeType, this.publishRoutingKey);
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		this.transcript.traceDebugging ("destroying the proxy...");
		this.transcript.traceDebugging ("destroying the underlying raw proxy...");
		return (this.raw.destroy ());
	}
	
	@Override
	public CallbackCompletion<Void> initialize ()
	{
		this.transcript.traceDebugging ("initializing the proxy...");
		final Callable<CallbackCompletion<Void>> initializeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call ()
			{
				AmqpQueuePublisherConnectorProxy.this.transcript.traceDebugging ("initializing the underlying raw proxy...");
				return (AmqpQueuePublisherConnectorProxy.this.raw.initialize ());
			}
		};
		final Callable<CallbackCompletion<Void>> declareExchangeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call ()
			{
				AmqpQueuePublisherConnectorProxy.this.transcript.traceDebugging ("declaring the exchange `%s` of type `%s`...", AmqpQueuePublisherConnectorProxy.this.exchange, AmqpQueuePublisherConnectorProxy.this.exchangeType);
				return (AmqpQueuePublisherConnectorProxy.this.raw.declareExchange (AmqpQueuePublisherConnectorProxy.this.exchange, AmqpQueuePublisherConnectorProxy.this.exchangeType, AmqpQueuePublisherConnectorProxy.this.exchangeDurable, AmqpQueuePublisherConnectorProxy.this.exchangeAutoDelete, AmqpQueuePublisherConnectorProxy.this.definePassive));
			}
		};
		// FIXME: If these operations fail we should continue with `destroy`.
		return (CallbackCompletionWorkflows.executeSequence (initializeOperation, declareExchangeOperation));
	}
	
	@Override
	public CallbackCompletion<Void> publish (final TMessage message)
	{
		Preconditions.checkNotNull (message);
		this.transcript.traceDebugging ("publishing a message to exchange `%s` (of type `%s`) with routing key `%s`...", this.exchange, this.exchangeType, this.publishRoutingKey);
		byte[] data = null;
		CallbackCompletion<Void> result = null;
		try {
			data = this.messageEncoder.encode (message);
		} catch (final EncodingException exception) {
			this.exceptions.traceDeferredException (exception, "encoding the message failed; deferring!");
			result = CallbackCompletion.createFailure (exception);
		}
		if (result == null) {
			final AmqpOutboundMessage outbound = new AmqpOutboundMessage (this.exchange, this.publishRoutingKey, data);
			result = this.raw.publish (outbound);
		}
		return (result);
	}
	
	public static <Message> AmqpQueuePublisherConnectorProxy<Message> create (final ConnectorConfiguration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder)
	{
		final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create (configuration);
		// FIXME: the splice below will be done when creating the environment
		//# final IConfiguration subConfiguration = configuration.spliceConfiguration(ConfigurationIdentifier.resolveRelative("publisher"));
		final AmqpQueuePublisherConnectorProxy<Message> proxy = new AmqpQueuePublisherConnectorProxy<Message> (rawProxy, configuration, messageClass, messageEncoder);
		return (proxy);
	}
	
	private final boolean definePassive;
	private final String exchange;
	private final boolean exchangeAutoDelete; // NOPMD
	private final boolean exchangeDurable;
	private final AmqpExchangeType exchangeType;
	private final String publishRoutingKey;
}
