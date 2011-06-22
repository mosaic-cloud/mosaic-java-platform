package mosaic.driver.interop.kvstore.memcached;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.IOperationType;
import mosaic.driver.interop.kvstore.KeyValueResponseTransmitter;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.OperationNames;

/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter extends KeyValueResponseTransmitter {

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
	public void sendResponse(CompletionToken token, IOperationType op,
			Object result, boolean isError) {

		if (!(op instanceof KeyValueOperations)) {
			return;
		}
		KeyValueOperations mOp = (KeyValueOperations) op;
		packAndSend(token, convertOperationType(mOp), result, isError);
	}

	private OperationNames convertOperationType(KeyValueOperations op) {
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
