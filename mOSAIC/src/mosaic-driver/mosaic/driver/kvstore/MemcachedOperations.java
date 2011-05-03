package mosaic.driver.kvstore;

import mosaic.core.ops.IOperationType;

/**
 * Operations supported by the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedOperations implements IOperationType {
	SET, ADD, REPLACE, APPEND, PREPEND, CAS, GET, GET_BULK, DELETE
}
