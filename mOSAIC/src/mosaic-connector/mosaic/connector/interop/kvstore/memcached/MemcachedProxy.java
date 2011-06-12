package mosaic.connector.interop.kvstore.memcached;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mosaic.connector.interop.ConnectorProxy;
import mosaic.connector.kvstore.memcached.MemcachedStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.StoreOperation;

/**
 * Proxy for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used by the {@link MemcachedStoreConnector}
 * to communicate with a memcached driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedProxy extends ConnectorProxy {
	private static final String DEFAULT_QUEUE_NAME = "memcached_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "memcached";

	/**
	 * Creates a proxy for memcached key-value distributed storage systems.
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
		super(config, connectorId, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME,
				reactor);
	}

	/**
	 * Returns a proxy for memcached key-value distributed storage systems.
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

	public synchronized void add(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.ADD, key, exp, data, handlers);
	}

	public synchronized void replace(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.REPLACE, key, exp, data, handlers);
	}

	public synchronized void append(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.APPEND, key, 0, data, handlers);
	}

	public synchronized void prepend(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.PREPEND, key, 0, data, handlers);
	}

	public synchronized void cas(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.CAS, key, 0, data, handlers);
	}

	public synchronized void get(String key,
			List<IOperationCompletionHandler<Object>> handlers) {
		List<String> keys = new ArrayList<String>();
		keys.add(key);
		sendGetMessage(OperationNames.GET, keys, handlers);
	}

	public synchronized void getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers) {
		sendGetMessage(OperationNames.GET_BULK, keys, handlers);
	}

	public synchronized void delete(String key,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());

		MosaicLogger.getLogger().trace(
				"MemcachedProxy - Sending " + OperationNames.DELETE.toString()
						+ " request [" + id + "]...");

		try {
			// store token and completion handlers
			super.registerHandlers(id, handlers);

			DeleteOperation op = new DeleteOperation();
			op.put(0, token);
			op.put(1, OperationNames.DELETE);
			op.put(2, key);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			super.sendRequest(message);
		} catch (IOException e) {
			for (IOperationCompletionHandler<Boolean> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send delete request to driver: " + e.getMessage(),
					e));
		}
	}

	public synchronized void list(
			List<IOperationCompletionHandler<List<String>>> handlers) {
		Exception e = new UnsupportedOperationException(
				"The memcached protocol does not support the LIST operation.");
		for (IOperationCompletionHandler<List<String>> handler : handlers) {
			handler.onFailure(e);
		}
		ExceptionTracer.traceDeferred(e);
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

	private <T extends Object> void sendGetMessage(OperationNames operation,
			List<String> keys, List<IOperationCompletionHandler<T>> handlers) {
		byte[] message;
		String id;

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

			GetOperation op = new GetOperation();
			op.put(0, token);
			op.put(1, operation);
			op.put(2, keys);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			super.sendRequest(message);
		} catch (IOException e) {
			for (IOperationCompletionHandler<T> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send get request to driver: " + e.getMessage(), e));
		}
	}
}
