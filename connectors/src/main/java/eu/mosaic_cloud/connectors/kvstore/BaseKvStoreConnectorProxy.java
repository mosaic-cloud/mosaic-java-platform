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
import java.util.List;

import eu.mosaic_cloud.connectors.core.BaseConnectorProxy;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.AbortRequest;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
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
public abstract class BaseKvStoreConnectorProxy<TValue extends Object> extends
        BaseConnectorProxy implements IKvStoreConnector<TValue> {

    protected DataEncoder<TValue> encoder;

    protected BaseKvStoreConnectorProxy(
            final ConnectorConfiguration configuration,
            final DataEncoder<TValue> encoder) {
        super(configuration);
        this.encoder = encoder;
    }

    @Override
    public CallbackCompletion<Boolean> delete(final String key) {
        final CompletionToken token = this.generateToken();
        final DeleteRequest.Builder requestBuilder = DeleteRequest.newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.setKey(key);
        final Message message = new Message(KeyValueMessage.DELETE_REQUEST,
                requestBuilder.build());
        return this.sendRequest(message, token, Boolean.class);
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        final CompletionToken token = this.generateToken();
        final AbortRequest.Builder requestBuilder = AbortRequest.newBuilder();
        requestBuilder.setToken(token);
        return this.disconnect(new Message(KeyValueMessage.ABORTED,
                requestBuilder.build()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CallbackCompletion<TValue> get(final String key) {
        return this.sendGetMessage(Arrays.asList(key),
                (Class<TValue>) Object.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CallbackCompletion<List<String>> list() {
        final CompletionToken token = this.generateToken();
        final ListRequest.Builder requestBuilder = ListRequest.newBuilder();
        requestBuilder.setToken(token);
        final Message message = new Message(KeyValueMessage.LIST_REQUEST,
                requestBuilder.build());
        return (CallbackCompletion<List<String>>) ((CallbackCompletion<?>) this
                .sendRequest(message, token, List.class));
    }

    @Override
    protected void processResponse(final Message message) {
        final KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
        switch (kvMessage) {
        case OK: {
            final IdlCommon.Ok okPayload = (Ok) message.payload;
            final CompletionToken token = okPayload.getToken();
            this.logger.debug(
                    "BaseKvStoreConnectorProxy - Received %s response [%s]...", // NOPMD
                    message.specification.toString(), token.getMessageId());
            this.pendingRequests.succeed(token.getMessageId(), Boolean.TRUE);
        }
            break;
        case NOK: {
            final IdlCommon.NotOk nokPayload = (NotOk) message.payload;
            final CompletionToken token = nokPayload.getToken();
            this.logger.debug(
                    "BaseKvStoreConnectorProxy - Received %s response [%s]...",
                    message.specification.toString(), token.getMessageId());
            this.pendingRequests.succeed(token.getMessageId(), Boolean.FALSE);
        }
            break;
        case ERROR: {
            final IdlCommon.Error errorPayload = (Error) message.payload;
            final CompletionToken token = errorPayload.getToken();
            this.logger.debug(
                    "BaseKvStoreConnectorProxy - Received %s response [%s]...",
                    message.specification.toString(), token.getMessageId());
            this.pendingRequests.fail(token.getMessageId(), new Exception(
                    errorPayload.getErrorMessage()));
        }
            break;
        case LIST_REPLY: {
            final KeyValuePayloads.ListReply listPayload = (ListReply) message.payload;
            final CompletionToken token = listPayload.getToken();
            this.logger.debug(
                    "BaseKvStoreConnectorProxy - Received %s response [%s]...",
                    message.specification.toString(), token.getMessageId());
            this.pendingRequests.succeed(token.getMessageId(),
                    listPayload.getKeysList());
        }
            break;
        case GET_REPLY: {
            final KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
            final CompletionToken token = getPayload.getToken();
            this.logger.debug(
                    "BaseKvStoreConnectorProxy - Received %s response [%s]...",
                    message.specification.toString(), token.getMessageId());
            final List<KVEntry> resultEntries = getPayload.getResultsList();
            TValue value = null; // NOPMD
            if (!resultEntries.isEmpty()) {
                try {
                    value = this.encoder.decode(resultEntries.get(0).getValue()
                            .toByteArray()); // NOPMD
                } catch (final EncodingException exception) {
                    this.pendingRequests.fail(token.getMessageId(), exception);
                    return;
                }
            }
            this.pendingRequests.succeed(token.getMessageId(), value);
        }
            break;
        default:
            break;
        }
    }

    protected <O> CallbackCompletion<O> sendGetMessage(final List<String> keys,
            final Class<O> outcomeClass) {
        final CompletionToken token = this.generateToken();
        final GetRequest.Builder requestBuilder = GetRequest.newBuilder();
        requestBuilder.setToken(token);
        requestBuilder.addAllKey(keys);
        final Message message = new Message(KeyValueMessage.GET_REQUEST,
                requestBuilder.build());
        return this.sendRequest(message, token, outcomeClass);
    }

    protected CallbackCompletion<Boolean> sendSetMessage(final String key,
            final TValue data, final int exp) {
        CallbackCompletion<Boolean> result;
        try {
            final byte[] dataBytes = this.encoder.encode(data);
            final CompletionToken token = this.generateToken();
            final SetRequest.Builder requestBuilder = SetRequest.newBuilder();
            requestBuilder.setToken(token);
            requestBuilder.setKey(key);
            requestBuilder.setExpTime(exp);
            requestBuilder.setValue(ByteString.copyFrom(dataBytes));
            final Message message = new Message(KeyValueMessage.SET_REQUEST,
                    requestBuilder.build());
            result = this.sendRequest(message, token, Boolean.class); // NOPMD
        } catch (final EncodingException exception) {
            result = CallbackCompletion.createFailure(exception);
        }
        return result;
    }

    public CallbackCompletion<Boolean> set(final String key, final int exp,
            final TValue data) {
        return this.sendSetMessage(key, data, exp);
    }

    @Override
    public CallbackCompletion<Boolean> set(final String key, final TValue data) {
        return this.set(key, 0, data);
    }
}
