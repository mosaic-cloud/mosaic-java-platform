package mosaic.connector.interop.kvstore.memcached;

import java.io.IOException;
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
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.IdlCommon.CompletionToken;
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
 * 
 */
public class MemcachedProxy extends KeyValueProxy {

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
	 * @throws Throwable
	 */
	private MemcachedProxy(IConfiguration config, String connectorId,
			MemcachedConnectorReactor reactor, ZeroMqChannel channel)
			throws Throwable {
		super(config, connectorId, reactor, channel);
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
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @return the proxy
	 * @throws Throwable
	 */
	public static MemcachedProxy create(IConfiguration config,
			String connectorIdentifier, String driverIdentifier,
			ZeroMqChannel channel) throws Throwable {
		String connectorId = connectorIdentifier;
		MemcachedConnectorReactor reactor = new MemcachedConnectorReactor(
				config);
		MemcachedProxy proxy = new MemcachedProxy(config, connectorId, reactor,
				channel);
		proxy.connect(driverIdentifier, MemcachedSession.CONNECTOR,
				new Message(KeyValueMessage.ACCESS, null));
		return proxy;
	}

	public synchronized void set(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendSetMessage(key, data, handlers, exp);
	}

	public void add(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.ADD_REQUEST, key, exp, data, handlers);
	}

	public synchronized void replace(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.REPLACE_REQUEST, key, exp, data,
				handlers);
	}

	public void append(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.APPEND_REQUEST, key, 0, data,
				handlers);
	}

	public void prepend(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.PREPEND_REQUEST, key, 0, data,
				handlers);
	}

	public void cas(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(MemcachedMessage.CAS_REQUEST, key, 0, data, handlers);
	}

	public void getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers) {
		sendGetMessage(keys, handlers);
	}

	@Override
	public void list(List<IOperationCompletionHandler<List<String>>> handlers) {
		Exception e = new UnsupportedOperationException(
				"The memcached protocol does not support the LIST operation.");
		for (IOperationCompletionHandler<List<String>> handler : handlers) {
			handler.onFailure(e);
		}
		// ExceptionTracer.traceDeferred(e);
	}

	private void sendStoreMessage(MemcachedMessage mcMessage, String key,
			int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		try {
			ByteString dataBytes = ByteString.copyFrom(SerDesUtils
					.toBytes(data));
			Message message = null;

			String id = UUID.randomUUID().toString();
			MosaicLogger.getLogger().trace(
					"KeyValueProxy - Sending " + mcMessage.toString()
							+ " request [" + id + "]...");

			// build token
			CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
			tokenBuilder.setMessageId(id);
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
			}

			// store token and completion handlers
			super.registerHandlers(id, handlers);

			super.sendRequest(
					getResponseReactor(MemcachedConnectorReactor.class)
							.getSession(), message);
		} catch (IOException e) {
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
