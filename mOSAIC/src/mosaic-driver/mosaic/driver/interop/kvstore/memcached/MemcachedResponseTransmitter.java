package mosaic.driver.interop.kvstore.memcached;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.interop.ResponseTransmitter;
import mosaic.driver.kvstore.memcached.MemcachedOperations;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.MemcachedError;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.OperationResponse;

/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter extends ResponseTransmitter {
	// private static final String DEFAULT_QUEUE_NAME = "memcached_responses";

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public MemcachedResponseTransmitter(IConfiguration config) {
		super(config);
	}

	/**
	 * Builds the result and sends it to the operation originator.
	 * 
	 * @param token
	 *            the token identifying the operation
	 * @param op
	 *            the identifier of the operation
	 * @param result
	 *            the result
	 * @param isError
	 *            <code>true</code> if the result is actual an error
	 */
	public void sendResponse(CompletionToken token, MemcachedOperations op,
			Object result, boolean isError) {
		byte[] dataBytes;
		byte[] message;
		String routingKey = ((CharSequence) token.get(1)).toString();

		MosaicLogger.getLogger().trace(
				"MemcachedResponseTransmitter: send response for " + op
						+ " request " + token.get(0).toString() + " client id "
						+ token.get(1).toString());

		OperationResponse response = new OperationResponse();
		try {
			response.put(0, token);
			response.put(1, convertOperationType(op));
			response.put(2, isError);
			if (!isError) {
				if (result instanceof Boolean) {
					response.put(3, (Boolean) result);
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> resMap = (Map<String, Object>) result;
					Map<CharSequence, ByteBuffer> sendMap = new HashMap<CharSequence, ByteBuffer>();
					for (Map.Entry<String, Object> entry : resMap.entrySet()) {
						dataBytes = SerDesUtils.toBytes(entry.getValue());
						ByteBuffer buff = ByteBuffer.wrap(dataBytes);
						sendMap.put(entry.getKey(), buff);
					}

					response.put(3, sendMap);
				}
			} else {
				MemcachedError error = new MemcachedError();
				error.put(0, (String) result);
				response.put(3, error);
			}
			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(routingKey, message);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

	private OperationNames convertOperationType(MemcachedOperations op) {
		OperationNames cOp = null;
		switch (op) {
		case ADD:
			cOp = OperationNames.ADD;
			break;
		case APPEND:
			cOp = OperationNames.APPEND;
			break;
		case CAS:
			cOp = OperationNames.CAS;
			break;
		case DELETE:
			cOp = OperationNames.DELETE;
			break;
		case GET:
			cOp = OperationNames.GET;
			break;
		case GET_BULK:
			cOp = OperationNames.GET_BULK;
			break;
		case PREPEND:
			cOp = OperationNames.PREPEND;
			break;
		case REPLACE:
			cOp = OperationNames.REPLACE;
			break;
		case SET:
			cOp = OperationNames.SET;
			break;
		}
		return cOp;
	}
}
