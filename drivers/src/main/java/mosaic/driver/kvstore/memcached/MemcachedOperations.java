package mosaic.driver.kvstore.memcached;

import mosaic.core.ops.IOperationType;

/**
 * Operations supported by the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedOperations implements IOperationType {
	ADD, REPLACE, APPEND, PREPEND, CAS, GET_BULK, LIST, GET, SET, DELETE;
}
