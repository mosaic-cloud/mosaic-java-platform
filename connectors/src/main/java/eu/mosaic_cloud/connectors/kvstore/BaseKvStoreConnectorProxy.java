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

package eu.mosaic_cloud.connectors.kvstore;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.connectors.core.BaseConnectorProxy;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.core.utils.EncodingMetadata;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.AbortRequest;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Envelope;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.DeleteRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import com.google.protobuf.ByteString;


/**
 * Proxy for the connector for key-value distributed storage systems. This is
 * used by the {@link BaseKvStoreConnector} to communicate with a key-value
 * store driver.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 * 
 */
public abstract class BaseKvStoreConnectorProxy<TValue extends Object>
		extends BaseConnectorProxy
		implements
			IKvStoreConnector<TValue>
{
	protected BaseKvStoreConnectorProxy (final ConnectorConfiguration configuration, final DataEncoder<TValue> encoder)
	{
		super (configuration);
		this.encoder = encoder;
	}
	
	@Override
	public CallbackCompletion<Void> delete (final String key)
	{
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("deleting the record with key `%s` (with request token `%s`)...", key, token.getMessageId ());
		final DeleteRequest.Builder requestBuilder = DeleteRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setKey (key);
		final Message message = new Message (KeyValueMessage.DELETE_REQUEST, requestBuilder.build ());
		return this.sendRequest (message, token, Void.class);
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		this.transcript.traceDebugging ("destroying the proxy...");
		final CompletionToken token = this.generateToken ();
		final AbortRequest.Builder requestBuilder = AbortRequest.newBuilder ();
		requestBuilder.setToken (token);
		return (this.disconnect (new Message (KeyValueMessage.ABORTED, requestBuilder.build ())));
	}
	
	@Override
	@SuppressWarnings ("unchecked")
	public CallbackCompletion<TValue> get (final String key)
	{
		return (this.sendGetRequest (Arrays.asList (key), (Class<TValue>) Object.class));
	}
	
	@Override
	@SuppressWarnings ("unchecked")
	public CallbackCompletion<List<String>> list ()
	{
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("listing the keys (with request token `%s`)...", token.getMessageId ());
		final ListRequest.Builder requestBuilder = ListRequest.newBuilder ();
		requestBuilder.setToken (token);
		final Message message = new Message (KeyValueMessage.LIST_REQUEST, requestBuilder.build ());
		return ((CallbackCompletion<List<String>>) ((CallbackCompletion<?>) this.sendRequest (message, token, List.class)));
	}
	
	public <TExtra extends MessageEnvelope> CallbackCompletion<Void> set (final String key, final int exp, final TValue data, final TExtra extra)
	{
		return this.sendSetRequest (key, data, exp, extra);
	}
	
	@Override
	public <TExtra extends MessageEnvelope> CallbackCompletion<Void> set (final String key, final TValue data, final TExtra extra)
	{
		return this.set (key, 0, data, extra);
	}
	
	@Override
	protected void processResponse (final Message message)
	{
		final KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		switch (kvMessage) {
			case OK : {
				final IdlCommon.Ok okPayload = (Ok) message.payload;
				final CompletionToken token = okPayload.getToken ();
				this.transcript.traceDebugging ("processing the success (OK) response for pending request with token `%s`...", token.getMessageId ());
				this.pendingRequests.succeed (token.getMessageId (), null);
			}
				break;
			case NOK : {
				final IdlCommon.NotOk nokPayload = (NotOk) message.payload;
				final CompletionToken token = nokPayload.getToken ();
				this.transcript.traceDebugging ("processing the failure (NOK) response for pending request with token `%s`...", token.getMessageId ());
				this.pendingRequests.fail (token.getMessageId (), new Exception ("request failed"));
			}
				break;
			case ERROR : {
				final IdlCommon.Error errorPayload = (Error) message.payload;
				final CompletionToken token = errorPayload.getToken ();
				this.transcript.traceDebugging ("processing the failure (error) response for pending request with token `%s` with message `%s`...", token.getMessageId (), errorPayload.getErrorMessage ());
				this.pendingRequests.fail (token.getMessageId (), new Exception (errorPayload.getErrorMessage ()));
			}
				break;
			case LIST_REPLY : {
				final KeyValuePayloads.ListReply listPayload = (ListReply) message.payload;
				final CompletionToken token = listPayload.getToken ();
				this.transcript.traceDebugging ("processing the success (list reply) response for pending request with token `%s`...", token.getMessageId ());
				this.pendingRequests.succeed (token.getMessageId (), listPayload.getKeysList ());
			}
				break;
			case GET_REPLY : {
				final KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
				final CompletionToken token = getPayload.getToken ();
				final List<KVEntry> resultEntries = getPayload.getResultsList ();
				this.transcript.traceDebugging ("processing the success (get reply) response (with `%d` entries) for pending request with token `%s`...", Integer.valueOf (resultEntries.size ()), token.getMessageId ());
				final Class<?> outcomeClass = this.pendingRequests.peek (token.getMessageId ()).future.outcomeClass;
				final Object outcome;
				if (outcomeClass == Map.class) {
					final Map<String, TValue> values = new HashMap<String, TValue> ();
					for (final KVEntry entry : resultEntries) {
						final Envelope envelope = entry.getEnvelope ();
						final EncodingMetadata encodingMetadata = new EncodingMetadata (envelope.getContentType (), envelope.getContentEncoding ());
						final TValue value;
						final byte[] rawValue = resultEntries.get (0).getValue ().toByteArray ();
						// FIXME: This `length > 0` should be handled differently...
						if ((rawValue != null) && (rawValue.length > 0)) {
							try {
								value = this.encoder.decode (rawValue, encodingMetadata);
							} catch (final EncodingException exception) {
								this.exceptions.traceDeferredException (exception, "decoding the value for record failed; deferring!");
								this.pendingRequests.fail (token.getMessageId (), exception);
								break;
							}
						} else {
							value = null;
						}
						values.put (entry.getKey (), value);
					}
					outcome = values;
				} else if (outcomeClass == Object.class) {
					final TValue value;
					if (!resultEntries.isEmpty ()) {
						final byte[] rawValue = resultEntries.get (0).getValue ().toByteArray ();
						// FIXME: This `length > 0` should be handled differently...
						if ((rawValue != null) && (rawValue.length > 0)) {
							final Envelope envelope = resultEntries.get (0).getEnvelope ();
							final EncodingMetadata encodingMetadata = new EncodingMetadata (envelope.getContentType (), envelope.getContentEncoding ());
							try {
								value = this.encoder.decode (rawValue, encodingMetadata);
							} catch (final EncodingException exception) {
								this.exceptions.traceDeferredException (exception, "decoding the value for record failed; deferring!");
								this.pendingRequests.fail (token.getMessageId (), exception);
								break;
							}
						} else {
							value = null;
						}
					} else {
						value = null;
					}
					outcome = value;
				} else {
					this.pendingRequests.fail (token.getMessageId (), new AssertionError ());
					break;
				}
				this.pendingRequests.succeed (token.getMessageId (), outcome);
			}
				break;
			default: {
				this.transcript.traceWarning ("processing unexpected message of type `%s`; ignoring...", message.specification);
			}
				break;
		}
	}
	
	protected <TOutcome> CallbackCompletion<TOutcome> sendGetRequest (final List<String> keys, final Class<TOutcome> outcomeClass)
	{
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("getting the record with key `%s` (and `%d` other keys) (with request token `%s`)...", keys.get (0), Integer.valueOf (keys.size () - 1), token.getMessageId ());
		final GetRequest.Builder requestBuilder = GetRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.addAllKey (keys);
		final Message message = new Message (KeyValueMessage.GET_REQUEST, requestBuilder.build ());
		return (this.sendRequest (message, token, outcomeClass));
	}
	
	protected <TExtra extends MessageEnvelope> CallbackCompletion<Void> sendSetRequest (final String key, final TValue data, final int exp, final TExtra extra)
	{
		final CompletionToken token = this.generateToken ();
		this.transcript.traceDebugging ("setting the record with key `%s` (with request token `%s`)...", key, token.getMessageId ());
		final SetRequest.Builder requestBuilder = SetRequest.newBuilder ();
		requestBuilder.setToken (token);
		requestBuilder.setKey (key);
		requestBuilder.setExpTime (exp);
		CallbackCompletion<Void> result = null;
		final EncodingMetadata encodingMetadata = extra.getEncodingMetadata ();
		final Envelope.Builder envelopeBuilder = Envelope.newBuilder ();
		if (null != encodingMetadata.getContentEncoding ())
			envelopeBuilder.setContentEncoding (encodingMetadata.getContentEncoding ());
		envelopeBuilder.setContentType (encodingMetadata.getContentType ());
		requestBuilder.setEnvelope (envelopeBuilder.build ());
		try {
			final byte[] dataBytes = this.encoder.encode (data, encodingMetadata);
			requestBuilder.setValue (ByteString.copyFrom (dataBytes));
		} catch (final EncodingException exception) {
			this.exceptions.traceDeferredException (exception, "encoding the value for record with key `%s` failed; deferring!", key);
			result = CallbackCompletion.createFailure (exception);
		}
		if (result == null) {
			final Message message = new Message (KeyValueMessage.SET_REQUEST, requestBuilder.build ());
			result = this.sendRequest (message, token, Void.class);
		}
		return (result);
	}
	
	protected final DataEncoder<TValue> encoder;
}
