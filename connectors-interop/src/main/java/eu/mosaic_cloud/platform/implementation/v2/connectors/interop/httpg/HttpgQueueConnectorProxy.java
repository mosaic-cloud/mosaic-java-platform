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

package eu.mosaic_cloud.platform.implementation.v2.connectors.interop.httpg;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.implementation.v2.connectors.interop.queue.amqp.AmqpQueueRawConnectorProxy;
import eu.mosaic_cloud.platform.implementation.v2.serialization.SerDesUtils;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgMessageToken;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueCallback;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgQueueConnector;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgRequestMessage;
import eu.mosaic_cloud.platform.v2.connectors.httpg.HttpgResponseMessage;
import eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueueRawConsumerCallback;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.platform.v2.serialization.EncodingException;
import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionWorkflows;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public final class HttpgQueueConnectorProxy<TRequestBody, TResponseBody>
			implements
				HttpgQueueConnector<TRequestBody, TResponseBody>
{
	private HttpgQueueConnectorProxy (final AmqpQueueRawConnectorProxy raw, final ConnectorConfiguration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueCallback<TRequestBody, TResponseBody> callback) {
		super ();
		Preconditions.checkNotNull (raw);
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (requestBodyClass);
		Preconditions.checkNotNull (requestBodyEncoder);
		Preconditions.checkNotNull (responseBodyClass);
		Preconditions.checkNotNull (responseBodyEncoder);
		this.raw = raw;
		final String identifier = this.raw.getIdentifier ();
		this.configuration = configuration;
		this.requestBodyClass = requestBodyClass;
		this.requestBodyEncoder = requestBodyEncoder;
		this.responseBodyClass = responseBodyClass;
		this.responseBodyEncoder = responseBodyEncoder;
		this.requestExchange = "mosaic-http-requests";
		this.requestExchangeType = AmqpExchangeType.TOPIC;
		this.requestExchangeDurable = false;
		this.requestExchangeAutoDelete = false;
		this.responseExchange = "mosaic-http-responses";
		this.responseExchangeType = AmqpExchangeType.DIRECT;
		this.responseExchangeDurable = false;
		this.responseExchangeAutoDelete = false;
		this.requestQueue = "mosaic-http-requests";
		this.requestQueueExclusive = false;
		this.requestQueueAutoDelete = false;
		this.requestQueueDurable = false;
		this.requestBindingRoutingKey = "#";
		this.requestConsumerIdentifier = identifier;
		this.requestConsumerAutoAck = false;
		this.definePassive = false;
		this.callback = new AmqpConsumerCallback (callback);
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
		this.transcript.traceDebugging ("created the httpg connector proxy");
		this.transcript.traceDebugging ("using the request queue `%s` bound to request exchange `%s` (of type `%s`) with routing key `%s`.", this.requestQueue, this.requestExchange, this.requestExchangeType, this.requestBindingRoutingKey);
		this.transcript.traceDebugging ("using the response exchange `%s` (of type `%s`).", this.responseExchange, this.responseExchangeType);
		this.transcript.traceDebugging ("using the underlying raw proxy `%{object:identity}`...", this.raw);
		this.transcript.traceDebugging ("using the underlying raw consumer callbacks `%{object:identity}`...", this.callback);
		this.transcript.traceDebugging ("using the delegate callbacks `%{object:identity}`...", this.callback.delegate);
	}
	
	@Override
	public CallbackCompletion<Void> destroy () {
		this.transcript.traceDebugging ("destroying the proxy...");
		final Callable<CallbackCompletion<Void>> cancelOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("canceling the consumer `%s`...", HttpgQueueConnectorProxy.this.requestConsumerIdentifier);
				return (HttpgQueueConnectorProxy.this.raw.cancel (HttpgQueueConnectorProxy.this.requestConsumerIdentifier));
			}
		};
		final Callable<CallbackCompletion<Void>> destroyOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("destroying the underlying raw proxy...");
				return (HttpgQueueConnectorProxy.this.raw.destroy ());
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
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("initializing the underlying raw proxy...");
				return (HttpgQueueConnectorProxy.this.raw.initialize ());
			}
		};
		final Callable<CallbackCompletion<Void>> declareRequestExchangeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("declaring the request exchange `%s` of type `%s`...", HttpgQueueConnectorProxy.this.requestExchange, HttpgQueueConnectorProxy.this.requestExchangeType);
				return (HttpgQueueConnectorProxy.this.raw.declareExchange (HttpgQueueConnectorProxy.this.requestExchange, HttpgQueueConnectorProxy.this.requestExchangeType, HttpgQueueConnectorProxy.this.requestExchangeDurable, HttpgQueueConnectorProxy.this.requestExchangeAutoDelete, HttpgQueueConnectorProxy.this.definePassive));
			}
		};
		final Callable<CallbackCompletion<Void>> declareResponseExchangeOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("declaring the response exchange `%s` of type `%s`...", HttpgQueueConnectorProxy.this.responseExchange, HttpgQueueConnectorProxy.this.responseExchangeType);
				return (HttpgQueueConnectorProxy.this.raw.declareExchange (HttpgQueueConnectorProxy.this.responseExchange, HttpgQueueConnectorProxy.this.responseExchangeType, HttpgQueueConnectorProxy.this.responseExchangeDurable, HttpgQueueConnectorProxy.this.responseExchangeAutoDelete, HttpgQueueConnectorProxy.this.definePassive));
			}
		};
		final Callable<CallbackCompletion<Void>> declareRequestQueueOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("declaring the request queue `%s`...", HttpgQueueConnectorProxy.this.requestQueue);
				return (HttpgQueueConnectorProxy.this.raw.declareQueue (HttpgQueueConnectorProxy.this.requestQueue, HttpgQueueConnectorProxy.this.requestQueueExclusive, HttpgQueueConnectorProxy.this.requestQueueDurable, HttpgQueueConnectorProxy.this.requestQueueAutoDelete, HttpgQueueConnectorProxy.this.definePassive));
			}
		};
		final Callable<CallbackCompletion<Void>> bindRequestQueueOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("binding the request queue `%s` to request exchange `%s` (of type `%s`) with routing key `%s`...", HttpgQueueConnectorProxy.this.requestQueue, HttpgQueueConnectorProxy.this.requestExchange, HttpgQueueConnectorProxy.this.requestExchangeType, HttpgQueueConnectorProxy.this.requestBindingRoutingKey);
				return (HttpgQueueConnectorProxy.this.raw.bindQueue (HttpgQueueConnectorProxy.this.requestExchange, HttpgQueueConnectorProxy.this.requestQueue, HttpgQueueConnectorProxy.this.requestBindingRoutingKey));
			}
		};
		final Callable<CallbackCompletion<Void>> consumeRequestOperation = new Callable<CallbackCompletion<Void>> () {
			@Override
			public CallbackCompletion<Void> call () {
				HttpgQueueConnectorProxy.this.transcript.traceDebugging ("registering the consumer `%s` for request queue `%s`...", HttpgQueueConnectorProxy.this.requestConsumerIdentifier, HttpgQueueConnectorProxy.this.requestQueue);
				return (HttpgQueueConnectorProxy.this.raw.consume (HttpgQueueConnectorProxy.this.requestQueue, HttpgQueueConnectorProxy.this.requestConsumerIdentifier, HttpgQueueConnectorProxy.this.requestQueueExclusive, HttpgQueueConnectorProxy.this.requestConsumerAutoAck, HttpgQueueConnectorProxy.this.callback));
			}
		};
		// FIXME: If these operations fail we should continue with `destroy`.
		return (CallbackCompletionWorkflows.executeSequence (initializeOperation, declareRequestExchangeOperation, declareResponseExchangeOperation, declareRequestQueueOperation, bindRequestQueueOperation, consumeRequestOperation));
	}
	
	@Override
	public CallbackCompletion<Void> respond (final HttpgResponseMessage<TResponseBody> response) {
		Preconditions.checkNotNull (response);
		final DeliveryToken delivery = (DeliveryToken) response.token;
		Preconditions.checkNotNull (delivery);
		Preconditions.checkArgument (delivery.proxy == this);
		final AmqpOutboundMessage outbound;
		try {
			outbound = this.encodeResponse (response);
		} catch (final EncodingException exception) {
			this.exceptions.traceDeferredException (exception, "encoding the message failed; deferring!");
			return (CallbackCompletion.createFailure (exception));
		}
		this.transcript.traceDebugging ("publishing a message to exchange `%s` (of type `%s`) with routing key `%s`...", this.responseExchange, this.responseExchangeType, delivery.callbackRoutingKey);
		final CallbackCompletion<Void> publishOutcome = this.raw.publish (outbound);
		this.transcript.traceDebugging ("acknowledging the message `%032x` for consumer `%s`...", Long.valueOf (delivery.acknowledgeToken), this.requestConsumerIdentifier);
		final CallbackCompletion<Void> acknowledgeOutcome = this.raw.ack (delivery.acknowledgeToken, false);
		return (CallbackCompletion.createChained (publishOutcome, acknowledgeOutcome));
	}
	
	protected HttpgRequestMessage<TRequestBody> decodeRequest (final AmqpInboundMessage message)
				throws EncodingException {
		try {
			final String contentType = message.getContentType ();
			if (!HttpgQueueConnectorProxy.expectedContentType.equals (contentType)) {
				throw (new EncodingException ("invalid content type"));
			}
			final String contentEncoding = message.getContentEncoding ();
			if (!HttpgQueueConnectorProxy.expectedContentEncoding.equals (contentEncoding)) {
				throw (new EncodingException ("invalid content encoding"));
			}
			final byte[] rawBytes = message.getData ();
			if (rawBytes.length < 4) {
				throw (new EncodingException ("invalid message length"));
			}
			final DataInputStream rawStream = new DataInputStream (new ByteArrayInputStream (rawBytes));
			final int metadataLength = rawStream.readInt ();
			if (metadataLength > rawBytes.length) {
				throw (new EncodingException ("invalid metadata length"));
			}
			final byte[] metadataBytes = new byte[metadataLength];
			rawStream.readFully (metadataBytes);
			final JSONObject metadata;
			try {
				metadata = SerDesUtils.jsonToRawObject (metadataBytes, Charsets.UTF_8);
			} catch (final JSONException exception) {
				throw (new EncodingException ("invalid metadata"));
			}
			final int version;
			try {
				version = metadata.getInt ("version");
			} catch (final JSONException exception) {
				throw (new EncodingException ("invalid metadata version", exception));
			}
			if (version != 1) {
				throw (new EncodingException (String.format ("unexpected metadata version `%d`", Integer.valueOf (version))));
			}
			final String callbackIdentifier;
			final String callbackExchange;
			final String callbackRoutingKey;
			try {
				callbackIdentifier = metadata.getString ("callback-identifier");
				callbackExchange = metadata.getString ("callback-exchange");
				callbackRoutingKey = metadata.getString ("callback-routing-key");
			} catch (final JSONException exception) {
				throw (new EncodingException ("invalid callback metadata", exception));
			}
			final String httpVersion;
			final String httpMethod;
			final String httpPath;
			final ImmutableMap<String, String> httpHeaders;
			final String httpBodyEncoding;
			try {
				httpVersion = metadata.getString ("http-version");
				httpMethod = metadata.getString ("http-method");
				httpPath = metadata.getString ("http-uri");
				final ImmutableMap.Builder<String, String> httpHeadersBuilder = ImmutableMap.<String, String> builder ();
				final JSONObject httpHeadersRaw = metadata.getJSONObject ("http-headers");
				final Iterator<?> httpHeadersIterator = httpHeadersRaw.keys ();
				while (httpHeadersIterator.hasNext ()) {
					final String httpHeaderName = ((String) httpHeadersIterator.next ());
					final String httpHeaderValue = httpHeadersRaw.getString (httpHeaderName);
					httpHeadersBuilder.put (httpHeaderName.toLowerCase (), httpHeaderValue);
				}
				httpHeaders = httpHeadersBuilder.build ();
				httpBodyEncoding = metadata.getString ("http-body");
			} catch (final JSONException exception) {
				throw (new EncodingException ("invalid http metadata", exception));
			}
			final byte[] httpBodyBytes;
			if (httpBodyEncoding.equals ("empty")) {
				httpBodyBytes = null;
			} else if (httpBodyEncoding.equals ("following")) {
				final int bodyLength = rawStream.readInt ();
				if (bodyLength != (rawBytes.length - metadataLength)) {
					throw (new EncodingException ("invalid body length"));
				}
				httpBodyBytes = new byte[bodyLength];
				rawStream.readFully (httpBodyBytes);
			} else if (httpBodyEncoding.equals ("embedded")) {
				try {
					httpBodyBytes = metadata.getString ("http-body-content").getBytes ();
				} catch (final JSONException exception) {
					throw (new EncodingException ("invalid http body", exception));
				}
			} else {
				throw (new EncodingException (String.format ("invalid body encoding `%s`", httpBodyEncoding)));
			}
			final EncodingMetadata bodyEncodingMetadata = new EncodingMetadata (httpHeaders.get ("content-type"), httpHeaders.get ("content-encoding"));
			final TRequestBody httpBody;
			if (httpBodyBytes != null) {
				try {
					httpBody = this.requestBodyEncoder.decode (httpBodyBytes, bodyEncodingMetadata);
				} catch (final EncodingException exception) {
					throw (new EncodingException ("invalid body", exception));
				}
			} else {
				httpBody = null;
			}
			final DeliveryToken token = new DeliveryToken (this, message.getDelivery (), callbackExchange, callbackRoutingKey, callbackIdentifier);
			final HttpgRequestMessage<TRequestBody> request = HttpgRequestMessage.create (httpVersion, httpMethod, httpPath, httpHeaders, httpBody, token);
			return (request);
		} catch (final EncodingException exception) {
			throw (exception);
		} catch (final Throwable exception) {
			throw (new EncodingException ("unexpected exception", exception));
		}
	}
	
	protected AmqpOutboundMessage encodeResponse (final HttpgResponseMessage<TResponseBody> response)
				throws EncodingException {
		try {
			final DeliveryToken token = (DeliveryToken) response.token;
			final JSONObject httpHeaders = new JSONObject ();
			String bodyContentType = null;
			String bodyContentEncoding = null;
			for (final Map.Entry<String, String> httpHeader : response.headers.entrySet ()) {
				final String httpHeaderName = httpHeader.getKey ().toLowerCase ();
				if (HttpgQueueConnectorProxy.ignoredHttpHeaders.contains (httpHeaderName)) {
					continue;
				}
				final String httpHeaderValue = httpHeader.getValue ();
				httpHeaders.put (httpHeaderName, httpHeaderValue);
				if (httpHeaderName.equals ("content-type")) {
					bodyContentType = httpHeaderValue;
				}
				if (httpHeaderName.equals ("content-encoding")) {
					bodyContentEncoding = httpHeaderValue;
				}
			}
			final JSONObject metadata = new JSONObject ();
			final EncodingMetadata httpBodyEncodingMetadata = new EncodingMetadata (bodyContentType, bodyContentEncoding);
			final byte[] httpBodyBytes;
			try {
				final EncodeOutcome outcome = this.responseBodyEncoder.encode (response.body, httpBodyEncodingMetadata);
				httpBodyBytes = outcome.data;
			} catch (final EncodingException exception) {
				throw (new EncodingException ("invalid body", exception));
			}
			metadata.put ("version", 1);
			metadata.put ("callback-identifier", token.callbackIdentifier);
			metadata.put ("http-version", response.version);
			metadata.put ("http-code", response.status);
			metadata.put ("http-status", "mosaic-http-response");
			metadata.put ("http-headers", httpHeaders);
			metadata.put ("http-body", "following");
			final byte[] metadataBytes;
			try {
				metadataBytes = SerDesUtils.toJsonBytes (metadata, Charsets.UTF_8);
			} catch (final JSONException exception) {
				throw (new EncodingException ("invalid metadata"));
			}
			final int messageLength = metadataBytes.length + httpBodyBytes.length + 8;
			final ByteArrayOutputStream rawBytesStream = new ByteArrayOutputStream (messageLength);
			final DataOutputStream rawStream = new DataOutputStream (rawBytesStream);
			rawStream.writeInt (metadataBytes.length);
			rawStream.write (metadataBytes);
			rawStream.writeInt (httpBodyBytes.length);
			rawStream.write (httpBodyBytes);
			final byte[] rawData = rawBytesStream.toByteArray ();
			final AmqpOutboundMessage rawMessage = new AmqpOutboundMessage (token.callbackExchange, token.callbackRoutingKey, rawData, HttpgQueueConnectorProxy.expectedContentEncoding, HttpgQueueConnectorProxy.expectedContentType);
			return (rawMessage);
		} catch (final EncodingException exception) {
			throw (exception);
		} catch (final Throwable exception) {
			throw (new EncodingException ("unexpected exception", exception));
		}
	}
	
	protected final ConnectorConfiguration configuration;
	protected final TranscriptExceptionTracer exceptions;
	protected final AmqpQueueRawConnectorProxy raw;
	protected final Class<TRequestBody> requestBodyClass;
	protected final DataEncoder<TRequestBody> requestBodyEncoder;
	protected final Class<TResponseBody> responseBodyClass;
	protected final DataEncoder<TResponseBody> responseBodyEncoder;
	protected final Transcript transcript;
	private final AmqpConsumerCallback callback;
	private final boolean definePassive;
	private final String requestBindingRoutingKey;
	private final boolean requestConsumerAutoAck;
	private final String requestConsumerIdentifier;
	private final String requestExchange;
	private final boolean requestExchangeAutoDelete;
	private final boolean requestExchangeDurable;
	private final AmqpExchangeType requestExchangeType;
	private final String requestQueue;
	private final boolean requestQueueAutoDelete;
	private final boolean requestQueueDurable;
	private final boolean requestQueueExclusive;
	private final String responseExchange;
	private final boolean responseExchangeAutoDelete;
	private final boolean responseExchangeDurable;
	private final AmqpExchangeType responseExchangeType;
	
	public static <TRequestBody, TResponseBody> HttpgQueueConnectorProxy<TRequestBody, TResponseBody> create (final ConnectorConfiguration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueCallback<TRequestBody, TResponseBody> callback) {
		final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create (configuration);
		final HttpgQueueConnectorProxy<TRequestBody, TResponseBody> proxy = new HttpgQueueConnectorProxy<TRequestBody, TResponseBody> (rawProxy, configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback);
		return (proxy);
	}
	
	public static final String expectedContentEncoding = "binary";
	public static final String expectedContentType = "application/octet-stream";
	protected static final ImmutableSet<String> ignoredHttpHeaders = ImmutableSet.of ("content-length", "connection");
	
	private final class AmqpConsumerCallback
				implements
					AmqpQueueRawConsumerCallback
	{
		AmqpConsumerCallback (final HttpgQueueCallback<TRequestBody, TResponseBody> delegate) {
			super ();
			this.delegate = delegate;
		}
		
		@Override
		public CallbackCompletion<Void> handleCancelOk (final String consumerTag) {
			HttpgQueueConnectorProxy.this.transcript.traceDebugging ("canceled the consumer `%s` successfully.", HttpgQueueConnectorProxy.this.requestConsumerIdentifier);
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleConsumeOk (final String consumerTag) {
			HttpgQueueConnectorProxy.this.transcript.traceDebugging ("registered the consumer `%s` successfully.", HttpgQueueConnectorProxy.this.requestConsumerIdentifier);
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public CallbackCompletion<Void> handleDelivery (final AmqpInboundMessage inbound) {
			HttpgQueueConnectorProxy.this.transcript.traceDebugging ("delivered the message `%032x` for consumer `%s`...", Long.valueOf (inbound.getDelivery ()), HttpgQueueConnectorProxy.this.requestConsumerIdentifier);
			HttpgRequestMessage<TRequestBody> request = null;
			try {
				request = HttpgQueueConnectorProxy.this.decodeRequest (inbound);
			} catch (final EncodingException exception) {
				HttpgQueueConnectorProxy.this.exceptions.traceDeferredException (exception, "decoding the message `%032x` failed; deferring!", Long.valueOf (inbound.getDelivery ()));
				return (CallbackCompletion.createFailure (exception));
			}
			HttpgQueueConnectorProxy.this.transcript.traceDebugging ("triggering callback for the message `%032x`...", Long.valueOf (inbound.getDelivery ()));
			return (this.delegate.requested (request));
		}
		
		@Override
		public CallbackCompletion<Void> handleShutdownSignal (final String consumerTag, final String message) {
			// FIXME: this should be handled...
			return (CallbackCompletion.createOutcome ());
		}
		
		final HttpgQueueCallback<TRequestBody, TResponseBody> delegate;
	}
	
	private static final class DeliveryToken
				implements
					HttpgMessageToken
	{
		DeliveryToken (final HttpgQueueConnectorProxy<?, ?> proxy, final long acknowledgeToken, final String callbackExchange, final String callbackRoutingKey, final String callbackIdentifier) {
			super ();
			this.proxy = proxy;
			this.acknowledgeToken = acknowledgeToken;
			this.callbackExchange = callbackExchange;
			this.callbackRoutingKey = callbackRoutingKey;
			this.callbackIdentifier = callbackIdentifier;
		}
		
		final long acknowledgeToken;
		final String callbackExchange;
		final String callbackIdentifier;
		final String callbackRoutingKey;
		final HttpgQueueConnectorProxy<?, ?> proxy;
	}
}
