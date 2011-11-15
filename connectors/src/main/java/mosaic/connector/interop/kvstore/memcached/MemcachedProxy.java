/*
 * #%L
 * mosaic-connector
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.connector.interop.kvstore.memcached;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import mosaic.connector.interop.kvstore.KeyValueProxy;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.DataEncoder;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import mosaic.interop.idl.kvstore.MemcachedPayloads;
import mosaic.interop.kvstore.KeyValueMessage;
import mosaic.interop.kvstore.MemcachedMessage;
import mosaic.interop.kvstore.MemcachedSession;

import com.google.protobuf.ByteString;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Proxy for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used by the {@link KeyValueStoreConnector} to
 * communicate with a memcached driver.
 * 
 * @author Georgiana Macariu
 * @param <T>
 *            type of stored data
 * 
 */
public final class MemcachedProxy<T extends Object> extends KeyValueProxy<T> {

	/**
	 * Creates a proxy for key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorId
	 *            the identifier of this connector's proxy
	 * @param reactor
	 *            the response reactor
	 * 
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @throws Throwable
	 */
	private MemcachedProxy(IConfiguration config, String connectorId,
			MemcachedConnectorReactor reactor, ZeroMqChannel channel,
			DataEncoder<T> encoder) throws Throwable {
		super(config, connectorId, reactor, channel, encoder);
	}

	/**
	 * Returns a proxy for key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorIdentifier
	 *            the identifier of this connector
	 * @param driverIdentifier
	 *            the identifier of the driver to which request will be sent
	 * @param bucket
	 *            the name of the bucket where the connector will operate
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @return the proxy
	 * @throws Throwable
	 */
	public static <T extends Object> MemcachedProxy<T> create(
			IConfiguration config, String connectorIdentifier,
			String driverIdentifier, String bucket, ZeroMqChannel channel,
			DataEncoder<T> encoder) throws Throwable {
		String connectorId = connectorIdentifier;
		MemcachedConnectorReactor reactor = new MemcachedConnectorReactor(
				encoder);
		MemcachedProxy<T> proxy = new MemcachedProxy<T>(config, connectorId,
				reactor, channel, encoder);

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(UUID.randomUUID().toString());
		tokenBuilder.setClientId(proxy.getConnectorId());

		// build request
		InitRequest.Builder requestBuilder = InitRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());
		requestBuilder.setBucket(bucket);

		proxy.connect(driverIdentifier, MemcachedSession.CONNECTOR,
				new Message(KeyValueMessage.ACCESS, requestBuilder.build()));
		return proxy;
	}

	public void set(String key, int exp, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendSetMessage(key, data, handlers, exp);
	}

	public void add(String key, int exp, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.ADD_REQUEST, key, exp, data, handlers);
	}

	public void replace(String key, int exp, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.REPLACE_REQUEST, key, exp, data,
				handlers);
	}

	public void append(String key, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.APPEND_REQUEST, key, 0, data,
				handlers);
	}

	public void prepend(String key, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.PREPEND_REQUEST, key, 0, data,
				handlers);
	}

	public void cas(String key, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.CAS_REQUEST, key, 0, data, handlers);
	}

	public void getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, T>>> handlers) {
		sendGetMessage(keys, handlers);
	}

	@Override
	public void list(List<IOperationCompletionHandler<List<String>>> handlers) {
		Exception exception = new UnsupportedOperationException( // NOPMD by georgiana on 10/13/11 12:42 PM
				"The memcached protocol does not support the LIST operation.");
		for (IOperationCompletionHandler<List<String>> handler : handlers) {
			handler.onFailure(exception);
		}
		// ExceptionTracer.traceDeferred(e);
	}

	private void sendStoreMessage(MemcachedMessage mcMessage, String key,
			int exp, T data, List<IOperationCompletionHandler<Boolean>> handlers) {
		try {
			ByteString dataBytes = ByteString.copyFrom(this.dataEncoder // NOPMD by georgiana on 10/13/11 12:42 PM
					.encode(data));
			Message message;

			String identifier = UUID.randomUUID().toString();
			MosaicLogger.getLogger().trace(
					"KeyValueProxy - Sending " + mcMessage.toString()
							+ " request [" + identifier + "]...");

			// build token
			CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
			tokenBuilder.setMessageId(identifier);
			tokenBuilder.setClientId(getConnectorId());

			// build message
			switch (mcMessage) {
			case ADD_REQUEST:
				MemcachedPayloads.AddRequest.Builder addBuilder = MemcachedPayloads.AddRequest
						.newBuilder();
				addBuilder.setToken(tokenBuilder.build());
				addBuilder.setKey(key);
				addBuilder.setExpTime(exp);
				addBuilder.setValue(dataBytes);
				message = new Message(MemcachedMessage.ADD_REQUEST,
						addBuilder.build());
				break;
			case APPEND_REQUEST:
				MemcachedPayloads.AppendRequest.Builder appendBuilder = MemcachedPayloads.AppendRequest
						.newBuilder();
				appendBuilder.setToken(tokenBuilder.build());
				appendBuilder.setKey(key);
				appendBuilder.setExpTime(exp);
				appendBuilder.setValue(dataBytes);
				message = new Message(MemcachedMessage.APPEND_REQUEST,
						appendBuilder.build());
				break;
			case PREPEND_REQUEST:
				MemcachedPayloads.PrependRequest.Builder prependBuilder = MemcachedPayloads.PrependRequest
						.newBuilder();
				prependBuilder.setToken(tokenBuilder.build());
				prependBuilder.setKey(key);
				prependBuilder.setExpTime(exp);
				prependBuilder.setValue(dataBytes);
				message = new Message(MemcachedMessage.PREPEND_REQUEST,
						prependBuilder.build());
				break;
			case CAS_REQUEST:
				MemcachedPayloads.CasRequest.Builder casBuilder = MemcachedPayloads.CasRequest
						.newBuilder();
				casBuilder.setToken(tokenBuilder.build());
				casBuilder.setKey(key);
				casBuilder.setExpTime(exp);
				casBuilder.setValue(dataBytes);
				message = new Message(MemcachedMessage.CAS_REQUEST,
						casBuilder.build());
				break;
			case REPLACE_REQUEST:
				MemcachedPayloads.ReplaceRequest.Builder replaceBuilder = MemcachedPayloads.ReplaceRequest
						.newBuilder();
				replaceBuilder.setToken(tokenBuilder.build());
				replaceBuilder.setKey(key);
				replaceBuilder.setExpTime(exp);
				replaceBuilder.setValue(dataBytes);
				message = new Message(MemcachedMessage.REPLACE_REQUEST,
						replaceBuilder.build());
				break;
			default:
				message = null; // NOPMD by georgiana on 10/13/11 12:46 PM
				break;
			}

			// store token and completion handlers
			super.registerHandlers(identifier, handlers);

			super.sendRequest(
					getResponseReactor(MemcachedConnectorReactor.class)
							.getSession(), message);
		} catch (Exception e) {
			for (IOperationCompletionHandler<Boolean> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer
					.traceDeferred(new ConnectionException(
							"Cannot send store request to driver: "
									+ e.getMessage(), e));
		}
	}

}
