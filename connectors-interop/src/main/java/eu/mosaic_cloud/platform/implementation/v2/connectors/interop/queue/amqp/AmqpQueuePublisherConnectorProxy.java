/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.platform.implementation.v2.connectors.interop.queue.amqp;


import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.implementation.v2.connectors.interop.tools.ConfigProperties;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.queue.QueuePublisherConnector;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.platform.v2.serialization.EncodingException;
import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionWorkflows;

import com.google.common.base.Preconditions;


public final class AmqpQueuePublisherConnectorProxy<TMessage>
			extends AmqpQueueConnectorProxy<TMessage>
			implements
				QueuePublisherConnector<TMessage>
{
	private AmqpQueuePublisherConnectorProxy (final AmqpQueueRawConnectorProxy rawProxy, final ConnectorConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder) {
		super (rawProxy, configuration, messageClass, messageEncoder);
		this.exchange = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_0, String.class, this.raw.getIdentifier ());
		this.exchangeType = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_5, AmqpExchangeType.class, AmqpExchangeType.DIRECT);
		this.exchangeDurable = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_9, Boolean.class, Boolean.FALSE).booleanValue ();
		this.exchangeAutoDelete = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_7, Boolean.class, Boolean.TRUE).booleanValue ();
		this.publishRoutingKey = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_1, String.class, this.raw.getIdentifier ());
		this.definePassive = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_8, Boolean.class, Boolean.FALSE).booleanValue ();
		this.transcript.traceDebugging ("created the queue publisher connector proxy for exchange `%s` (of type `%s`) with routing key `%s`.", this.exchange, this.exchangeType, this.publishRoutingKey);
	}
	
	@Override
	public CallbackCompletion<Void> destroy () {
		this.transcript.traceDebugging ("destroying the proxy...");
		this.transcript.traceDebugging ("destroying the underlying raw proxy...");
		return this.raw.destroy ();
	}
	
	@Override
	public CallbackCompletion<Void> initialize () {
		this.transcript.traceDebugging ("initializing the proxy...");
		final Callable<CallbackCompletion<Void>> initializeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueuePublisherConnectorProxy.this.transcript.traceDebugging ("initializing the underlying raw proxy...");
				return (AmqpQueuePublisherConnectorProxy.this.raw.initialize ());
			}
		};
		final Callable<CallbackCompletion<Void>> declareExchangeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueuePublisherConnectorProxy.this.transcript.traceDebugging ("declaring the exchange `%s` of type `%s`...", AmqpQueuePublisherConnectorProxy.this.exchange, AmqpQueuePublisherConnectorProxy.this.exchangeType);
				return (AmqpQueuePublisherConnectorProxy.this.raw.declareExchange (AmqpQueuePublisherConnectorProxy.this.exchange, AmqpQueuePublisherConnectorProxy.this.exchangeType, AmqpQueuePublisherConnectorProxy.this.exchangeDurable, AmqpQueuePublisherConnectorProxy.this.exchangeAutoDelete, AmqpQueuePublisherConnectorProxy.this.definePassive));
			}
		};
		// FIXME: If these operations fail we should continue with `destroy`.
		return CallbackCompletionWorkflows.executeSequence (initializeOperation, declareExchangeOperation);
	}
	
	@Override
	public CallbackCompletion<Void> publish (final TMessage message) {
		Preconditions.checkNotNull (message);
		this.transcript.traceDebugging ("publishing a message to exchange `%s` (of type `%s`) with routing key `%s`...", this.exchange, this.exchangeType, this.publishRoutingKey);
		byte[] data = null;
		String contentType = null;
		String contentEncoding = null;
		CallbackCompletion<Void> result = null;
		try {
			final EncodeOutcome outcome = this.messageEncoder.encode (message, null);
			data = outcome.data;
			if (outcome.metadata.hasContentEncoding () && !EncodingMetadata.ANY.hasSameContentEncoding (outcome.metadata)) {
				contentEncoding = outcome.metadata.getContentEncoding ();
			}
			if (outcome.metadata.hasContentType () && !EncodingMetadata.ANY.hasSameContentType (outcome.metadata)) {
				contentType = outcome.metadata.getContentType ();
			}
		} catch (final EncodingException exception) {
			this.exceptions.traceDeferredException (exception, "encoding the message failed; deferring!");
			result = CallbackCompletion.createFailure (exception);
		}
		if (result == null) {
			final AmqpOutboundMessage outbound = new AmqpOutboundMessage (this.exchange, this.publishRoutingKey, data, contentEncoding, contentType);
			result = this.raw.publish (outbound);
		}
		return (result);
	}
	
	private final boolean definePassive;
	private final String exchange;
	private final boolean exchangeAutoDelete;
	private final boolean exchangeDurable;
	private final AmqpExchangeType exchangeType;
	private final String publishRoutingKey;
	
	public static <TMessage> AmqpQueuePublisherConnectorProxy<TMessage> create (final ConnectorConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder) {
		final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create (configuration);
		// FIXME: the splice below will be done when creating the environment
		//# final Configuration subConfiguration = configuration.spliceConfiguration(ConfigurationIdentifier.resolveRelative("publisher"));
		final AmqpQueuePublisherConnectorProxy<TMessage> proxy = new AmqpQueuePublisherConnectorProxy<TMessage> (rawProxy, configuration, messageClass, messageEncoder);
		return proxy;
	}
}
