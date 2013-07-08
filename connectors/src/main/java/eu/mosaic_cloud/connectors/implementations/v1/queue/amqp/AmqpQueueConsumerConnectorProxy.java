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

package eu.mosaic_cloud.connectors.implementations.v1.queue.amqp;


import java.util.concurrent.Callable;

import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.implementations.v1.tools.ConfigProperties;
import eu.mosaic_cloud.connectors.v1.queue.amqp.AmqpMessageToken;
import eu.mosaic_cloud.connectors.v1.queue.amqp.AmqpQueueConsumerCallback;
import eu.mosaic_cloud.connectors.v1.queue.amqp.AmqpQueueRawConsumerCallback;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingException;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionWorkflows;

import com.google.common.base.Preconditions;


public final class AmqpQueueConsumerConnectorProxy<TMessage>
			extends AmqpQueueConnectorProxy<TMessage>
			implements
				IAmqpQueueConsumerConnector<TMessage>
{
	private AmqpQueueConsumerConnectorProxy (final AmqpQueueRawConnectorProxy rawProxy, final ConnectorConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerCallback<TMessage> callback) {
		super (rawProxy, configuration, messageClass, messageEncoder);
		final String identifier = this.raw.getIdentifier ();
		this.exchange = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_0, String.class, identifier);
		this.exchangeType = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_5, AmqpExchangeType.class, AmqpExchangeType.DIRECT);
		this.exchangeDurable = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_9, Boolean.class, Boolean.FALSE).booleanValue ();
		this.exchangeAutoDelete = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_7, Boolean.class, Boolean.TRUE).booleanValue ();
		this.queue = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_2, String.class, identifier);
		this.queueExclusive = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_6, Boolean.class, Boolean.FALSE).booleanValue ();
		this.queueAutoDelete = this.exchangeAutoDelete;
		this.queueDurable = this.exchangeDurable;
		this.bindingRoutingKey = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_1, String.class, identifier);
		// FIXME: this should also be made a configurable parameter...
		this.consumerIdentifier = identifier;
		this.consumerAutoAck = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_10, Boolean.class, Boolean.FALSE).booleanValue ();
		this.definePassive = configuration.getConfigParameter (ConfigProperties.AmqpQueueConnector_8, Boolean.class, Boolean.FALSE).booleanValue ();
		this.callback = new AmqpConsumerCallback (callback);
		this.transcript.traceDebugging ("created the queue consumer connector proxy for queue `%s` bound to exchange `%s` (of type `%s`) with routing key `%s`.", this.queue, this.exchange, this.exchangeType, this.bindingRoutingKey);
		this.transcript.traceDebugging ("using the underlying raw proxy `%{object:identity}`...", this.raw);
		this.transcript.traceDebugging ("using the underlying raw consumer callbacks `%{object:identity}`...", this.callback);
		this.transcript.traceDebugging ("using the delegate consumer callbacks `%{object:identity}`...", this.callback.delegate);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final AmqpMessageToken token_) {
		final DeliveryToken token = (DeliveryToken) token_;
		Preconditions.checkNotNull (token);
		Preconditions.checkArgument (token.proxy == this);
		this.transcript.traceDebugging ("acknowledging the message `%s` for consumer `%s`...", token, this.consumerIdentifier);
		return (this.raw.ack (token.getDelivery (), false));
	}
	
	@Override
	public CallbackCompletion<Void> destroy () {
		this.transcript.traceDebugging ("destroying the proxy...");
		final Callable<CallbackCompletion<Void>> cancelOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("canceling the consumer `%s`...", AmqpQueueConsumerConnectorProxy.this.consumerIdentifier);
				return (AmqpQueueConsumerConnectorProxy.this.raw.cancel (AmqpQueueConsumerConnectorProxy.this.consumerIdentifier));
			}
		};
		final Callable<CallbackCompletion<Void>> destroyOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("destroying the underlying raw proxy...");
				return (AmqpQueueConsumerConnectorProxy.this.raw.destroy ());
			}
		};
		return (CallbackCompletionWorkflows.executeSequence (cancelOperation, destroyOperation));
	}
	
	@Override
	public CallbackCompletion<Void> initialize () {
		this.transcript.traceDebugging ("initializing the proxy...");
		final Callable<CallbackCompletion<Void>> initializeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("initializing the underlying raw proxy...");
				return (AmqpQueueConsumerConnectorProxy.this.raw.initialize ());
			}
		};
		final Callable<CallbackCompletion<Void>> declareExchangeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("declaring the exchange `%s` of type `%s`...", AmqpQueueConsumerConnectorProxy.this.exchange, AmqpQueueConsumerConnectorProxy.this.exchangeType);
				return (AmqpQueueConsumerConnectorProxy.this.raw.declareExchange (AmqpQueueConsumerConnectorProxy.this.exchange, AmqpQueueConsumerConnectorProxy.this.exchangeType, AmqpQueueConsumerConnectorProxy.this.exchangeDurable, AmqpQueueConsumerConnectorProxy.this.exchangeAutoDelete, AmqpQueueConsumerConnectorProxy.this.definePassive));
			}
		};
		final Callable<CallbackCompletion<Void>> declareQueueOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("declaring the queue `%s`...", AmqpQueueConsumerConnectorProxy.this.queue);
				return (AmqpQueueConsumerConnectorProxy.this.raw.declareQueue (AmqpQueueConsumerConnectorProxy.this.queue, AmqpQueueConsumerConnectorProxy.this.queueExclusive, AmqpQueueConsumerConnectorProxy.this.queueDurable, AmqpQueueConsumerConnectorProxy.this.queueAutoDelete, AmqpQueueConsumerConnectorProxy.this.definePassive));
			}
		};
		final Callable<CallbackCompletion<Void>> bindQueueOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("binding the queue `%s` to exchange `%s` (of type `%s`) with routing key `%s`...", AmqpQueueConsumerConnectorProxy.this.queue, AmqpQueueConsumerConnectorProxy.this.exchange, AmqpQueueConsumerConnectorProxy.this.exchangeType, AmqpQueueConsumerConnectorProxy.this.bindingRoutingKey);
				return (AmqpQueueConsumerConnectorProxy.this.raw.bindQueue (AmqpQueueConsumerConnectorProxy.this.exchange, AmqpQueueConsumerConnectorProxy.this.queue, AmqpQueueConsumerConnectorProxy.this.bindingRoutingKey));
			}
		};
		final Callable<CallbackCompletion<Void>> consumeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("registering the consumer `%s` for queue `%s`...", AmqpQueueConsumerConnectorProxy.this.consumerIdentifier, AmqpQueueConsumerConnectorProxy.this.queue);
				return (AmqpQueueConsumerConnectorProxy.this.raw.consume (AmqpQueueConsumerConnectorProxy.this.queue, AmqpQueueConsumerConnectorProxy.this.consumerIdentifier, AmqpQueueConsumerConnectorProxy.this.queueExclusive, AmqpQueueConsumerConnectorProxy.this.consumerAutoAck, AmqpQueueConsumerConnectorProxy.this.callback));
			}
		};
		// FIXME: If these operations fail we should continue with `destroy`.
		return (CallbackCompletionWorkflows.executeSequence (initializeOperation, declareExchangeOperation, declareQueueOperation, bindQueueOperation, consumeOperation));
	}
	
	private final String bindingRoutingKey;
	private final AmqpConsumerCallback callback;
	private final boolean consumerAutoAck;
	private final String consumerIdentifier;
	private final boolean definePassive;
	private final String exchange;
	private final boolean exchangeAutoDelete;
	private final boolean exchangeDurable;
	private final AmqpExchangeType exchangeType;
	private final String queue;
	private final boolean queueAutoDelete;
	private final boolean queueDurable;
	private final boolean queueExclusive;
	
	public static <TMessage> AmqpQueueConsumerConnectorProxy<TMessage> create (final ConnectorConfiguration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerCallback<TMessage> callback) {
		final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create (configuration);
		// FIXME: the splice below will be done when creating the environment
		//# final Configuration subConfiguration = configuration.spliceConfiguration(ConfigurationIdentifier.resolveRelative("publisher"));
		final AmqpQueueConsumerConnectorProxy<TMessage> proxy = new AmqpQueueConsumerConnectorProxy<TMessage> (rawProxy, configuration, messageClass, messageEncoder, callback);
		return (proxy);
	}
	
	private final class AmqpConsumerCallback
				implements
					AmqpQueueRawConsumerCallback
	{
		AmqpConsumerCallback (final AmqpQueueConsumerCallback<TMessage> delegate) {
			super ();
			this.delegate = delegate;
		}
		
		@Override
		public CallbackCompletion<Void> handleCancelOk (final String consumerTag) {
			AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("canceled the consumer `%s` successfully.", AmqpQueueConsumerConnectorProxy.this.consumerIdentifier);
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleConsumeOk (final String consumerTag) {
			AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("registered the consumer `%s` successfully.", AmqpQueueConsumerConnectorProxy.this.consumerIdentifier);
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleDelivery (final AmqpInboundMessage inbound) {
			final DeliveryToken token = new DeliveryToken (AmqpQueueConsumerConnectorProxy.this, inbound.getDelivery ());
			final byte[] data = inbound.getData ();
			AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("delivered the message `%s` for consumer `%s`...", token, AmqpQueueConsumerConnectorProxy.this.consumerIdentifier);
			TMessage message = null;
			CallbackCompletion<Void> result = null;
			final EncodingMetadata encodingMetadata = new EncodingMetadata (inbound.getContentType (), inbound.getContentEncoding ());
			try {
				message = AmqpQueueConsumerConnectorProxy.this.messageEncoder.decode (data, encodingMetadata);
			} catch (final EncodingException exception) {
				AmqpQueueConsumerConnectorProxy.this.exceptions.traceDeferredException (exception, "decoding the message `%s` failed; deferring!", token);
				result = CallbackCompletion.createFailure (exception);
			}
			if (result == null) {
				AmqpQueueConsumerConnectorProxy.this.transcript.traceDebugging ("triggering callback for the message `%s`...", token);
				result = this.delegate.consume (token, message);
			}
			return (result);
		}
		
		@Override
		public CallbackCompletion<Void> handleShutdownSignal (final String consumerTag, final String message) {
			// FIXME: this should be handled...
			return (CallbackCompletion.createOutcome ());
		}
		
		final AmqpQueueConsumerCallback<TMessage> delegate;
	}
	
	private static final class DeliveryToken
				implements
					AmqpMessageToken
	{
		DeliveryToken (final AmqpQueueConsumerConnectorProxy<?> proxy, final long token) {
			super ();
			this.proxy = proxy;
			this.token = token;
		}
		
		@Override
		public String toString () {
			return (String.format ("%032x", Long.valueOf (this.token)));
		}
		
		long getDelivery () {
			return (this.token);
		}
		
		final AmqpQueueConsumerConnectorProxy<?> proxy;
		final long token;
	}
}
