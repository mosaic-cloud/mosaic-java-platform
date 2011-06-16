package mosaic.connector.interop.kvstore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mosaic.connector.interop.ConnectorProxy;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.ListOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.SetOperation;

/**
 * Proxy for the driver for key-value distributed storage systems. This is used
 * by the {@link KeyValueStoreConnector} to communicate with a key-value store
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueProxy extends ConnectorProxy {
	private static final String DEFAULT_QUEUE_NAME = "kvstore_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "kvstore";

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
	protected KeyValueProxy(IConfiguration config, String connectorId,
			KeyValueConnectorReactor reactor) throws Throwable {
		super(config, connectorId, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME,
				reactor);
	}

	/**
	 * Returns a proxy for key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @return the proxy
	 * @throws Throwable
	 */
	public static KeyValueProxy create(IConfiguration config) throws Throwable {
		String connectorId = UUID.randomUUID().toString(); // FIXME this should
															// be
		// replaced
		KeyValueConnectorReactor reactor = new KeyValueConnectorReactor(config,
				connectorId);
		return new KeyValueProxy(config, connectorId, reactor);
	}

	public void set(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendSetMessage(OperationNames.SET, key, data, handlers);
	}

	public void get(String key,
			List<IOperationCompletionHandler<Object>> handlers) {
		List<String> keys = new ArrayList<String>();
		keys.add(key);
		sendGetMessage(OperationNames.GET, keys, handlers);
	}

	public void delete(String key,
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

	public void list(List<IOperationCompletionHandler<List<String>>> handlers) {
		sendListMessage(OperationNames.LIST, handlers);
	}

	protected void sendListMessage(OperationNames operation,
			List<IOperationCompletionHandler<List<String>>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());
		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending " + operation.toString()
						+ " request [" + id + "]...");
		try {
			// store token and completion handlers
			super.registerHandlers(id, handlers);

			ListOperation op = new ListOperation();
			op.put(0, token);
			op.put(1, operation);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			super.sendRequest(message);
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

	protected void sendSetMessage(OperationNames operation, String key,
			Object data, List<IOperationCompletionHandler<Boolean>> handlers) {
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
				"KeyValueProxy - Sending " + operation.toString()
						+ " request [" + id + "]...");
		try {
			// store token and completion handlers
			super.registerHandlers(id, handlers);

			dataBytes = SerDesUtils.toBytes(data);
			buff = ByteBuffer.wrap(dataBytes);
			SetOperation op = new SetOperation();
			op.put(0, token);
			op.put(1, operation);
			op.put(2, key);
			op.put(3, buff);
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
					"Cannot send set request to driver: " + e.getMessage(), e));
		}
	}

	protected <T extends Object> void sendGetMessage(OperationNames operation,
			List<String> keys, List<IOperationCompletionHandler<T>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());

		MosaicLogger.getLogger().trace(
				"KeyValueProxy - Sending " + operation.toString()
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
