package mosaic.connector.interop.kvstore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mosaic.connector.interop.AbstractConnectorReactor;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.MemcachedError;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.OperationResponse;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * key-value store connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueConnectorReactor extends AbstractConnectorReactor {
	private static final String DEFAULT_QUEUE_NAME = "kvstore_responses";
	private static final String DEFAULT_EXCHANGE_NAME = "kvstore";

	/**
	 * Creates the reactor for the key-value store connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param bindingKey
	 *            queue binding key
	 * @throws Throwable
	 */
	protected KeyValueConnectorReactor(IConfiguration config, String bindingKey)
			throws Throwable {
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
		case SET:
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
				ExceptionTracer.traceDeferred(e);
			}

			break;
		case LIST:
			List<CharSequence> resultList = (List<CharSequence>) response
					.get(3);
			List<String> resList = new ArrayList<String>();
			for (CharSequence key : resultList) {
				resList.add(key.toString());
			}
			for (IOperationCompletionHandler<?> handler : handlers) {
				((IOperationCompletionHandler<List<String>>) handler)
						.onSuccess(resList);
			}
			break;
		default:
			break;
		}
	}
}
