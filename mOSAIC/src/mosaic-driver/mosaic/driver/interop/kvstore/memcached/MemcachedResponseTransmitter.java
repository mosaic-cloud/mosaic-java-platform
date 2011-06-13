package mosaic.driver.interop.kvstore.memcached;

import mosaic.core.configuration.IConfiguration;
import mosaic.driver.interop.kvstore.KeyValueResponseTransmitter;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.kvstore.OperationNames;

/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter extends KeyValueResponseTransmitter {
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
