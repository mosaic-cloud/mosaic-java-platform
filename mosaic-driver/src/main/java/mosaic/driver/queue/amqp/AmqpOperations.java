package mosaic.driver.queue.amqp;

import mosaic.core.ops.IOperationType;

/**
 * Operations defined for the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum AmqpOperations implements IOperationType {
	DECLARE_EXCHANGE, DECLARE_QUEUE, BIND_QUEUE, CONSUME, PUBLISH, GET, ACK, CANCEL;
	private static final AmqpOperations[] copyOfValues = values();

	/**
	 * Tests if given operation is supported by driver.
	 * 
	 * @param operation
	 *            name of operation
	 * @return <code>true</code> if operation is supported
	 */
	public static boolean isOperation(String operation) {
		for (AmqpOperations op : AmqpOperations.copyOfValues) {
			if (op.name().equalsIgnoreCase(operation))
				return true;
		}
		return false;
	}
}
