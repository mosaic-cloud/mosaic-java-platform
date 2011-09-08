package mosaic.driver.kvstore;

import mosaic.core.ops.IOperationType;

/**
 * Basic operations supported by a key-value stores and memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum KeyValueOperations implements IOperationType {
	SET, GET, LIST, DELETE, ADD, REPLACE, APPEND, PREPEND, CAS, GET_BULK;

}
