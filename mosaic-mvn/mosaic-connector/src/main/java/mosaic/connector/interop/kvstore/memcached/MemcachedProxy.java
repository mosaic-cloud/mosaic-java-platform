package mosaic.connector.interop.kvstore.memcached;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.StoreOperation;

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
	 * @throws Throwable
	 */
	private MemcachedProxy(IConfiguration config, String connectorId,
			MemcachedConnectorReactor reactor) throws Throwable {
		super(config, connectorId, reactor);
	}

	/**
	 * Returns a proxy for key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @return the proxy
	 * @throws Throwable
	 */
	public static MemcachedProxy create(IConfiguration config) throws Throwable {
		String connectorId = UUID.randomUUID().toString(); // FIXME this should
															// be
		// replaced
		MemcachedConnectorReactor reactor = new MemcachedConnectorReactor(
				config, connectorId);
		return new MemcachedProxy(config, connectorId, reactor);
	}

	public synchronized void set(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.SET, key, exp, data, handlers);
	}

	public void add(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.ADD, key, exp, data, handlers);
	}

	public synchronized void replace(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.REPLACE, key, exp, data, handlers);
	}

	public void append(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.APPEND, key, 0, data, handlers);
	}

	public void prepend(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.PREPEND, key, 0, data, handlers);
	}

	public void cas(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.CAS, key, 0, data, handlers);
	}

	public void getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers) {
		sendGetMessage(OperationNames.GET_BULK, keys, handlers);
	}

	public void list(List<IOperationCompletionHandler<List<String>>> handlers) {
		Exception e = new UnsupportedOperationException(
				"The memcached protocol does not support the LIST operation.");
		for (IOperationCompletionHandler<List<String>> handler : handlers) {
			handler.onFailure(e);
		}
//		ExceptionTracer.traceDeferred(e);
	}

	private void sendStoreMessage(OperationNames operation, String key,
			int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		byte[] dataBytes;
		byte[] message;
		String id;
		ByteBuffer buff;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());
		MosaicLogger.getLogger().trace(
				"MemcachedProxy - Sending " + operation.toString()
						+ " request [" + id + "]...");
		try {
			// store token and completion handlers
			super.registerHandlers(id, handlers);

			dataBytes = SerDesUtils.toBytes(data);
			buff = ByteBuffer.wrap(dataBytes);
			StoreOperation op = new StoreOperation();
			op.put(0, token);
			op.put(1, operation);
			op.put(2, key);
			op.put(3, exp);
			op.put(4, buff);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			super.sendRequest(message);
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
