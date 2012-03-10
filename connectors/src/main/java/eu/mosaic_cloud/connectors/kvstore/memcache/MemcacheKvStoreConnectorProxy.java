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

package eu.mosaic_cloud.connectors.kvstore.memcache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnectorProxy;
import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import com.google.protobuf.ByteString;

/**
 * Proxy for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used by the {@link GenericKvStoreConnector}
 * to communicate with a memcached driver.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 * 
 */
public final class MemcacheKvStoreConnectorProxy<TValue extends Object> extends
        BaseKvStoreConnectorProxy<TValue> implements IMemcacheKvStoreConnector<TValue> {

    protected MemcacheKvStoreConnectorProxy(final IConfiguration configuration,
            final ConnectorEnvironment environment, final DataEncoder<TValue> encoder) {
        super(configuration, environment, encoder);
    }

    /**
     * Returns a proxy for key-value distributed storage systems.
     * 
     * @param bucket
     *            the name of the bucket where the connector will operate
     * @param configuration
     *            the configurations required to initialize the proxy
     * @param driverIdentity
     *            the identifier of the driver to which request will be sent
     * @param channel
     *            the channel on which to communicate with the driver
     * @param encoder
     *            encoder used for serializing and deserializing data stored in
     *            the key-value store
     * @return the proxy
     */
    public static <T extends Object> MemcacheKvStoreConnectorProxy<T> create(
            final IConfiguration configuration, final ConnectorEnvironment environment,
            final DataEncoder<T> encoder) {
        final MemcacheKvStoreConnectorProxy<T> proxy = new MemcacheKvStoreConnectorProxy<T>(
                configuration, environment, encoder);
        return proxy;
    }

    @Override
    public CallbackCompletion<Boolean> add(final String key, final int exp, final TValue data) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final MemcachedPayloads.AddRequest.Builder requestBuilder = MemcachedPayloads.AddRequest
                    .newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            requestBuilder.setExpTime(exp);
            final Message message = new Message(MemcachedMessage.ADD_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class);
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }

    @Override
    public CallbackCompletion<Boolean> append(final String key, final TValue data) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final MemcachedPayloads.AppendRequest.Builder requestBuilder = MemcachedPayloads.AppendRequest
                    .newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            final Message message = new Message(MemcachedMessage.APPEND_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class);
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }

    @Override
    public CallbackCompletion<Boolean> cas(final String key, final TValue data) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final MemcachedPayloads.CasRequest.Builder requestBuilder = MemcachedPayloads.CasRequest
                    .newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            final Message message = new Message(MemcachedMessage.CAS_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class);
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CallbackCompletion<Map<String, TValue>> getBulk(final List<String> keys) {
        return this.sendGetMessage(keys, (Class<Map<String, TValue>>) ((Class<?>) Map.class));
    }

    @Override
    public CallbackCompletion<Void> initialize() {
        final String bucket = ConfigUtils.resolveParameter(this.getConfiguration(),
                ConfigProperties.getString("GenericKvStoreConnector.1"), String.class, "");
        final InitRequest.Builder requestBuilder = InitRequest.newBuilder();
        requestBuilder.setToken(this.generateToken());
        requestBuilder.setBucket(bucket);
        return this.connect(MemcachedSession.CONNECTOR, new Message(KeyValueMessage.ACCESS,
                requestBuilder.build()));
    }

    @Override
    public CallbackCompletion<Boolean> prepend(final String key, final TValue data) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final MemcachedPayloads.PrependRequest.Builder requestBuilder = MemcachedPayloads.PrependRequest
                    .newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            final Message message = new Message(MemcachedMessage.PREPEND_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class);
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }

    @Override
    protected void processResponse(final Message message) {
        final KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
        if (kvMessage == KeyValueMessage.GET_REPLY) {
            final KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
            final CompletionToken token = getPayload.getToken();
            this.logger.debug("KvStoreConnectorProxy - Received "
                    + message.specification.toString() + " response [" + token.getMessageId()
                    + "]...");
            if (this.pendingRequests.peek(token.getMessageId()).future.outcomeClass == Map.class) {
                final List<KVEntry> resultEntries = getPayload.getResultsList();
                final Map<String, Object> values = new HashMap<String, Object>();
                for (final KVEntry entry : resultEntries) {
                    try {
                        final TValue value = this.encoder.decode(resultEntries.get(0).getValue()
                                .toByteArray());
                        values.put(entry.getKey(), value);
                    } catch (final EncodingException exception) {
                        this.pendingRequests.fail(token.getMessageId(), exception);
                        return;
                    }
                }
                this.pendingRequests.succeed(token.getMessageId(), values);
            }
            super.processResponse(message);
        } else {
            super.processResponse(message);
        }
    }

    @Override
    public CallbackCompletion<Boolean> replace(final String key, final int exp, final TValue data) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final MemcachedPayloads.ReplaceRequest.Builder requestBuilder = MemcachedPayloads.ReplaceRequest
                    .newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setExpTime(exp);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            final Message message = new Message(MemcachedMessage.REPLACE_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class);
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }
}
