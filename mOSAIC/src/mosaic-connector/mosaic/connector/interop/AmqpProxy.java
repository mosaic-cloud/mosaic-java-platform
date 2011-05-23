package mosaic.connector.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import mosaic.connector.queue.AmqpConnector;
import mosaic.connector.queue.AmqpOutboundMessage;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.queue.AmqpExchangeType;
import mosaic.interop.idl.amqp.AckOperation;
import mosaic.interop.idl.amqp.BindQueueOperation;
import mosaic.interop.idl.amqp.CancelOperation;
import mosaic.interop.idl.amqp.CloseConnectionOperation;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.ConsumeOperation;
import mosaic.interop.idl.amqp.DeclareExchangeOperation;
import mosaic.interop.idl.amqp.DeclareQueueOperation;
import mosaic.interop.idl.amqp.ExchangeType;
import mosaic.interop.idl.amqp.GetOperation;
import mosaic.interop.idl.amqp.OpenConnectionOperation;
import mosaic.interop.idl.amqp.Operation;
import mosaic.interop.idl.amqp.OperationNames;
import mosaic.interop.idl.amqp.PublishOperation;

import org.apache.avro.specific.SpecificRecord;

/**
 * Proxy for the driver for queuing systems implementing the AMQP protocol. This
 * is used by the {@link AmqpConnector} to communicate with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpProxy extends ConnectorProxy {
	private static final String DEFAULT_QUEUE_NAME = "aqmp_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "amqp";

	/**
	 * Creates a proxy for AMQP queuing systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorId
	 *            the identifier of this connector's proxy
	 * @param reactor
	 *            the response reactor
	 */
	private AmqpProxy(IConfiguration config, String connectorId,
			AmqpConnectorReactor reactor) {
		super(config, connectorId, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME,
				reactor);

	}

	/**
	 * Returns a proxy for AMQP queuing systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @return the proxy
	 */
	public static AmqpProxy create(IConfiguration config) {
		String connectorId = UUID.randomUUID().toString(); // FIXME this should
		// be replaced
		AmqpConnectorReactor reactor = new AmqpConnectorReactor(config,
				connectorId);
		return new AmqpProxy(config, connectorId, reactor);
	}

	public synchronized void openConnection(
			List<IOperationCompletionHandler<Boolean>> handlers) {
		OpenConnectionOperation op = new OpenConnectionOperation();
		sendRequest(op, OperationNames.OPEN_CONNECTION, handlers);
	}

	public synchronized void closeConnection(
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CloseConnectionOperation op = new CloseConnectionOperation();
		sendRequest(op, OperationNames.CLOSE_CONNECTION, handlers);
	}

	public synchronized void declareExchange(String name,
			AmqpExchangeType type, boolean durable, boolean autoDelete,
			boolean passive, List<IOperationCompletionHandler<Boolean>> handlers) {
		ExchangeType eType = ExchangeType
				.valueOf(type.toString().toUpperCase());

		DeclareExchangeOperation op = new DeclareExchangeOperation();
		op.put(0, name);
		op.put(1, eType);
		op.put(2, durable);
		op.put(3, autoDelete);
		op.put(4, passive);
		sendRequest(op, OperationNames.DECLARE_EXCHANGE, handlers);
	}

	public synchronized void declareQueue(String queue, boolean exclusive,
			boolean durable, boolean autoDelete, boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		DeclareQueueOperation op = new DeclareQueueOperation();
		op.put(0, queue);
		op.put(1, exclusive);
		op.put(2, durable);
		op.put(3, autoDelete);
		op.put(4, passive);
		sendRequest(op, OperationNames.DECLARE_QUEUE, handlers);
	}

	public synchronized void bindQueue(String exchange, String queue,
			String routingKey,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		BindQueueOperation op = new BindQueueOperation();
		op.put(0, exchange);
		op.put(1, queue);
		op.put(2, routingKey);
		sendRequest(op, OperationNames.BIND_QUEUE, handlers);
	}

	public synchronized void publish(AmqpOutboundMessage message,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		PublishOperation op = new PublishOperation();
		op.put(0, message.getCallback());
		op.put(1, message.getContentEncoding());
		op.put(2, message.getContentType());
		op.put(3, message.getCorrelation());
		op.put(4, message.getData());
		op.put(5, message.isDurable());
		op.put(6, message.getExchange());
		op.put(7, message.getIdentifier());
		op.put(8, message.isImmediate());
		op.put(9, message.isMandatory());
		op.put(10, message.getRoutingKey());
		sendRequest(op, OperationNames.PUBLISH, handlers);
	}

	public synchronized void consume(String queue, String consumer,
			boolean exclusive, boolean autoAck, Object extra,
			List<IOperationCompletionHandler<String>> handlers) {

		byte[] dataBytes;
		try {
			dataBytes = SerDesUtils.toBytes(extra);
			ByteBuffer buff = ByteBuffer.wrap(dataBytes);
			ConsumeOperation op = new ConsumeOperation();
			op.put(0, queue);
			op.put(1, consumer);
			op.put(2, exclusive);
			op.put(3, autoAck);
			op.put(4, buff);
			sendRequest(op, OperationNames.CONSUME, handlers);
		} catch (IOException e) {
			e.printStackTrace();
			ExceptionTracer
					.traceRethrown(new ConnectionException(
							"Cannot send consume request to driver: "
									+ e.getMessage()));
		}

	}

	public synchronized void cancel(String consumer,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		CancelOperation op = new CancelOperation();
		op.put(0, consumer);
		sendRequest(op, OperationNames.CANCEL, handlers);
	}

	public synchronized void get(String queue, boolean autoAck,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		GetOperation op = new GetOperation();
		op.put(0, queue);
		op.put(1, autoAck);
		sendRequest(op, OperationNames.GET, handlers);
	}

	public synchronized void ack(long delivery, boolean multiple,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		AckOperation op = new AckOperation();
		op.put(0, delivery);
		op.put(1, multiple);
		sendRequest(op, OperationNames.ACK, handlers);
	}

	private <T extends SpecificRecord, V extends Object> void sendRequest(T operation,
			OperationNames opName,
			List<IOperationCompletionHandler<V>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());
		MosaicLogger.getLogger().trace(
				"Sending " + opName.toString() + " request [" + id + "]...");
		try {
			// store token and completion handlers
			super.registerHandlers(id, handlers);

			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, token);
			enclosingOperation.put(1, opName);
			enclosingOperation.put(2, operation);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			super.sendRequest(message);
		} catch (IOException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"Cannot send " + opName.toString() + " request to driver: "
							+ e.getMessage()));
		}
	}

}
