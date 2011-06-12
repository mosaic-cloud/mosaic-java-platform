package mosaic.driver.kvstore.memcached;

import mosaic.core.ops.IOperationType;

/**
 * Operations supported by the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum MemcachedOperations implements IOperationType {
	SET, ADD, REPLACE, APPEND, PREPEND, CAS, GET, GET_BULK, DELETE;

	private static final MemcachedOperations[] copyOfValues = values();

	/**
	 * Tests if given operation is supported by driver.
	 * 
	 * @param operation
	 *            name of operation
	 * @return <code>true</code> if operation is supported
	 */
	public static boolean isOperation(String operation) {
		for (MemcachedOperations op : copyOfValues) {
			if (op.name().equalsIgnoreCase(operation))
				return true;
		}
		return false;
	}
}
