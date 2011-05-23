package mosaic.driver.queue;

import mosaic.core.ops.IOperationType;

/**
 * Operations defined for the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum AmqpOperations implements IOperationType {
	OPEN_CONNECTION, CLOSE_CONNECTION, DECLARE_EXCHANGE, DECLARE_QUEUE, BIND_QUEUE, CONSUME, PUBLISH, GET, ACK, CANCEL
}
