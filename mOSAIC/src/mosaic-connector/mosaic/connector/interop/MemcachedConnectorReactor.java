package mosaic.connector.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.MemcachedError;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.OperationResponse;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * Memcached connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedConnectorReactor extends AbstractConnectorReactor {
	private static final String DEFAULT_QUEUE_NAME = "memcached_responses";
	private static final String DEFAULT_EXCHANGE_NAME = "memcached";

	/**
	 * Creates the reactor for the Memcached connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param bindingKey
	 *            queue binding key
	 */
	protected MemcachedConnectorReactor(IConfiguration config, String bindingKey) {
		super(config, bindingKey, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.connector.interop.AbstractConnectorReactor#processResponse(byte[])
	 */
	@SuppressWarnings("unchecked")
	protected void processResponse(byte[] message) throws IOException {
		OperationResponse response = new OperationResponse();
		response = SerDesUtils.deserializeWithSchema(message, response);
		CompletionToken token = (CompletionToken) response.get(0);
		OperationNames op = (OperationNames) response.get(1);
		boolean isError = (Boolean) response.get(2);
		String id = ((CharSequence) token.get(0)).toString();

		List<IOperationCompletionHandler<?>> handlers = super.getDispatcher()
				.removeRequestHandlers(id);
		if (handlers == null) {
			MosaicLogger.getLogger().error(
					"No handler found for request token: " + id);
			return;
		}
		ByteBuffer buff;
		Object data;

		if (isError) {
			MemcachedError error = (MemcachedError) response.get(3);
			for (IOperationCompletionHandler<?> handler : handlers) {
				handler.onFailure(new Exception(((CharSequence) error.get(0))
						.toString()));
			}
			return;
		}

		switch (op) {
		case ADD:
		case SET:
		case APPEND:
		case PREPEND:
		case CAS:
		case REPLACE:
		case DELETE:
			Boolean resultB = (Boolean) response.get(3);
			for (IOperationCompletionHandler<?> handler : handlers) {
				((IOperationCompletionHandler<Boolean>) handler)
						.onSuccess(resultB);
			}
			break;
		case GET:
			Map<CharSequence, ByteBuffer> resultO = (Map<CharSequence, ByteBuffer>) response
					.get(3);
			buff = resultO.values().toArray(new ByteBuffer[0])[0];

			try {
				data = null;
				if (buff != null)
					data = SerDesUtils.toObject(buff.array());
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<Object>) handler)
							.onSuccess(data);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				MosaicLogger.getLogger().error(e.getMessage());
			}

			break;
		case GET_BULK:
			Map<CharSequence, ByteBuffer> resultM = (Map<CharSequence, ByteBuffer>) response
					.get(3);
			Map<String, Object> resMap = new HashMap<String, Object>();
			try {
				for (Map.Entry<CharSequence, ByteBuffer> entry : resultM
						.entrySet()) {
					buff = entry.getValue();
					data = SerDesUtils.toObject(buff.array());
					resMap.put(entry.getKey().toString(), data);
				}
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<Map<String, Object>>) handler)
							.onSuccess(resMap);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				MosaicLogger.getLogger().error(e.getMessage());
			}
			break;
		default:
			break;
		}
	}
}
