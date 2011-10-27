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
package mosaic.connector.interop.kvstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mosaic.connector.interop.AbstractConnectorReactor;
import mosaic.connector.interop.ConnectorProxy;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.DataEncoder;
import mosaic.interop.idl.IdlCommon.AbortRequest;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.kvstore.KeyValuePayloads.DeleteRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.ListRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import mosaic.interop.kvstore.KeyValueMessage;
import mosaic.interop.kvstore.KeyValueSession;

import com.google.protobuf.ByteString;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Proxy for the driver for key-value distributed storage systems. This is used
 * by the {@link KeyValueStoreConnector} to communicate with a key-value store
 * driver.
 * 
 * @author Georgiana Macariu
 * @param <T>
 *            type of stored data
 * 
 */
public class KeyValueProxy<T extends Object> extends ConnectorProxy {

	protected DataEncoder<T> dataEncoder;

	/**
	 * Creates a proxy for key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorId
	 *            the identifier of this connector's proxy
	 * @param reactor
	 *            the response reactor
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @throws Throwable
	 */
	protected KeyValueProxy(IConfiguration config, String connectorId,
			AbstractConnectorReactor reactor, ZeroMqChannel channel,
			DataEncoder<T> encoder) throws Throwable {
		super(config, connectorId, reactor, channel);
		this.dataEncoder = encoder;
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
	public static <T extends Object> KeyValueProxy<T> create(
			IConfiguration config, String connectorIdentifier,
			String driverIdentifier, String bucket, ZeroMqChannel channel,
			DataEncoder<T> encoder) throws Throwable {
		String connectorId = connectorIdentifier;
		AbstractConnectorReactor reactor = new KeyValueConnectorReactor(encoder);
		KeyValueProxy<T> proxy = new KeyValueProxy<T>(config, connectorId,
				reactor, channel, encoder);

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(UUID.randomUUID().toString());
		tokenBuilder.setClientId(proxy.getConnectorId());

		// build request
		InitRequest.Builder requestBuilder = InitRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());
		requestBuilder.setBucket(bucket);

		proxy.connect(driverIdentifier, KeyValueSession.CONNECTOR, new Message(
				KeyValueMessage.ACCESS, requestBuilder.build()));
		return proxy;
	}

	@Override
	public void destroy() throws Throwable {
		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(UUID.randomUUID().toString());
		tokenBuilder.setClientId(getConnectorId());

		// build request
		AbortRequest.Builder requestBuilder = AbortRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());

		synchronized (this) {
			super.sendRequest(
					getResponseReactor(KeyValueConnectorReactor.class)
							.getSession(), new Message(KeyValueMessage.ABORTED,
							requestBuilder.build()));
			super.destroy();
		}
	}

	public void set(String key, T data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendSetMessage(key, data, handlers);
	}

	public void get(String key, List<IOperationCompletionHandler<T>> handlers) {
		List<String> keys = new ArrayList<String>();
		keys.add(key);
		sendGetMessage(keys, handlers);
	}

	public void delete(String key,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		Message message;

		String identifier = UUID.randomUUID().toString();

		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending DELETE request [" + identifier
						+ "]..."); // NOPMD by georgiana on 10/13/11 12:36 PM

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(identifier);
		tokenBuilder.setClientId(getConnectorId());
		// build request
		DeleteRequest.Builder requestBuilder = DeleteRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());
		requestBuilder.setKey(key);

		message = new Message(KeyValueMessage.DELETE_REQUEST,
				requestBuilder.build());

		// store token and completion handlers
		super.registerHandlers(identifier, handlers);

		try {
			super.sendRequest(
					getResponseReactor(KeyValueConnectorReactor.class)
							.getSession(), message);
		} catch (IOException e) {
			for (IOperationCompletionHandler<Boolean> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send delete request to driver: " + e.getMessage(),
					e));
		}
	}

	public void list(List<IOperationCompletionHandler<List<String>>> handlers) {
		String identifier = UUID.randomUUID().toString();
		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending LIST request [" + identifier + "]...");

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(identifier);
		tokenBuilder.setClientId(getConnectorId());

		// build request
		ListRequest.Builder requestBuilder = ListRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());

		Message message = new Message(KeyValueMessage.LIST_REQUEST,
				requestBuilder.build());

		// store token and completion handlers
		super.registerHandlers(identifier, handlers);
		try {
			super.sendRequest(
					getResponseReactor(KeyValueConnectorReactor.class)
							.getSession(), message);
		} catch (IOException e) {
			for (IOperationCompletionHandler<List<String>> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer
					.traceDeferred(new ConnectionException(
							"Cannot send list request to driver: "
									+ e.getMessage(), e));
		}
	}

	protected void sendSetMessage(String key, T data,
			List<IOperationCompletionHandler<Boolean>> handlers, Integer... exp) {
		String identifier = UUID.randomUUID().toString();
		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending SET request [" + identifier + "]...");

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(identifier);
		tokenBuilder.setClientId(getConnectorId());

		try {
			// build request
			SetRequest.Builder requestBuilder = SetRequest.newBuilder();
			requestBuilder.setToken(tokenBuilder.build());
			requestBuilder.setKey(key);
			if (exp.length > 0) {
				requestBuilder.setExpTime(exp[0]);
			}

			byte[] dataBytes = this.dataEncoder.encode(data);
			requestBuilder.setValue(ByteString.copyFrom(dataBytes));

			Message message = new Message(KeyValueMessage.SET_REQUEST,
					requestBuilder.build());

			// store token and completion handlers
			super.registerHandlers(identifier, handlers);

			super.sendRequest(
					getResponseReactor(KeyValueConnectorReactor.class)
							.getSession(), message);
		} catch (Exception e) {
			for (IOperationCompletionHandler<Boolean> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send set request to driver: " + e.getMessage(), e));
		}
	}

	protected <D extends Object> void sendGetMessage(List<String> keys,
			List<IOperationCompletionHandler<D>> handlers) {
		String identifier = UUID.randomUUID().toString();
		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending GET request [" + identifier + "]...");

		// build token
		CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
		tokenBuilder.setMessageId(identifier);
		tokenBuilder.setClientId(getConnectorId());

		// build request
		GetRequest.Builder requestBuilder = GetRequest.newBuilder();
		requestBuilder.setToken(tokenBuilder.build());
		requestBuilder.addAllKey(keys);

		Message message = new Message(KeyValueMessage.GET_REQUEST,
				requestBuilder.build());

		// store token and completion handlers
		super.registerHandlers(identifier, handlers);

		try {
			super.sendRequest(
					getResponseReactor(KeyValueConnectorReactor.class)
							.getSession(), message);
		} catch (IOException e) {
			for (IOperationCompletionHandler<D> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send get request to driver: " + e.getMessage(), e));
		}
	}
}
