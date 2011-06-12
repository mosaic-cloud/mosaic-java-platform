package mosaic.connector.interop.queue.amqp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import mosaic.connector.interop.ConnectorProxy;
import mosaic.connector.queue.amqp.AmqpConnector;
import mosaic.connector.queue.amqp.IAmqpConsumerCallback;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.queue.amqp.AmqpExchangeType;
import mosaic.driver.queue.amqp.AmqpOutboundMessage;
import mosaic.interop.idl.amqp.AckOperation;
import mosaic.interop.idl.amqp.BindQueueOperation;
import mosaic.interop.idl.amqp.CancelOperation;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.ConsumeOperation;
import mosaic.interop.idl.amqp.DeclareExchangeOperation;
import mosaic.interop.idl.amqp.DeclareQueueOperation;
import mosaic.interop.idl.amqp.ExchangeType;
import mosaic.interop.idl.amqp.GetOperation;
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
	private static final String DEFAULT_QUEUE_NAME = "amqp_requests";
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
	 * @throws Throwable
	 */
	private AmqpProxy(IConfiguration config, String connectorId,
			AmqpConnectorReactor reactor) throws Throwable {
		super(config, connectorId, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME,
				reactor);
	}

	/**
	 * Returns a proxy for AMQP queuing systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @return the proxy
	 * @throws Throwable
	 */
	public static AmqpProxy create(IConfiguration config) throws Throwable {
		// FIXME next instruction should be replaced in the future
		String connectorId = UUID.randomUUID().toString();

		AmqpConnectorReactor reactor = new AmqpConnectorReactor(config,
				connectorId);
		AmqpProxy proxy = new AmqpProxy(config, connectorId, reactor);

		// // open connection
		// final CountDownLatch waitOpenConn = new CountDownLatch(1);
		// final AtomicBoolean opened = new AtomicBoolean(false);
		// IOperationCompletionHandler<Boolean> openConnHandler = new
		// IOperationCompletionHandler<Boolean>() {
		//
		// @Override
		// public void onSuccess(Boolean result) {
		// opened.set(result);
		// waitOpenConn.countDown();
		// }
		//
		// @Override
		// public <E extends Throwable> void onFailure(E error) {
		// waitOpenConn.countDown();
		// }
		// };
		// OpenConnectionOperation op = new OpenConnectionOperation();
		// List<IOperationCompletionHandler<Boolean>> handlers = new
		// ArrayList<IOperationCompletionHandler<Boolean>>();
		// handlers.add(openConnHandler);
		// proxy.sendRequest(op, OperationNames.OPEN_CONNECTION, handlers);
		// waitOpenConn.await();
		//
		// if (!opened.get())
		// proxy = null;

		return proxy;
	}

	public void destroy() throws Throwable {
		// close connection
		// final CountDownLatch waitOpenConn = new CountDownLatch(1);
		// final AtomicBoolean closed = new AtomicBoolean(false);
		// IOperationCompletionHandler<Boolean> closeConnHandler = new
		// IOperationCompletionHandler<Boolean>() {
		//
		// @Override
		// public void onSuccess(Boolean result) {
		// closed.set(result);
		// waitOpenConn.countDown();
		// }
		//
		// @Override
		// public <E extends Throwable> void onFailure(E error) {
		// waitOpenConn.countDown();
		// }
		// };
		// CloseConnectionOperation op = new CloseConnectionOperation();
		// List<IOperationCompletionHandler<Boolean>> handlers = new
		// ArrayList<IOperationCompletionHandler<Boolean>>();
		// handlers.add(closeConnHandler);
		// sendRequest(op, OperationNames.CLOSE_CONNECTION, handlers);
		// waitOpenConn.await();

		super.destroy();
	}

	// public synchronized void openConnection(
	// List<IOperationCompletionHandler<Boolean>> handlers) {
	// OpenConnectionOperation op = new OpenConnectionOperation();
	// sendRequest(op, OperationNames.OPEN_CONNECTION, handlers);
	// }
	//
	// public synchronized void closeConnection(
	// List<IOperationCompletionHandler<Boolean>> handlers) {
	// CloseConnectionOperation op = new CloseConnectionOperation();
	// sendRequest(op, OperationNames.CLOSE_CONNECTION, handlers);
	// }

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
		ByteBuffer buff = ByteBuffer.wrap(message.getData());
		PublishOperation op = new PublishOperation();
		// op.put(0, message.getCallback());
		// op.put(1, message.getContentEncoding());
		// op.put(2, message.getContentType());
		// op.put(3, message.getCorrelation());
		// op.put(4, buff);
		// op.put(5, message.isDurable());
		// op.put(6, message.getExchange());
		// op.put(7, message.getIdentifier());
		// op.put(8, message.isImmediate());
		// op.put(9, message.isMandatory());
		// op.put(10, message.getRoutingKey());
		op.put(0, buff);
		op.put(1, message.isDurable());
		op.put(2, message.getExchange());
		op.put(3, message.isImmediate());
		op.put(4, message.isMandatory());
		op.put(5, message.getRoutingKey());
		sendRequest(op, OperationNames.PUBLISH, handlers);
	}

	public synchronized void consume(String queue, String consumer,
			boolean exclusive, boolean autoAck, Object extra,
			List<IOperationCompletionHandler<String>> handlers,
			IAmqpConsumerCallback consumerCallback) {

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
			String requestId = sendRequest(op, OperationNames.CONSUME, handlers);
			AmqpConnectorReactor reactor = super
					.getResponseReactor(AmqpConnectorReactor.class);
			// if (consumer.equals(""))
			// reactor.addCallback(requestId, consumerCallback);
			// else
			reactor.addCallback(consumer, consumerCallback);
		} catch (IOException e) {
			for (IOperationCompletionHandler<String> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send consume request to driver: " + e.getMessage(),
					e));
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

	private <T extends SpecificRecord, V extends Object> String sendRequest(
			T operation, OperationNames opName,
			List<IOperationCompletionHandler<V>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, super.getConnectorId());

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
			MosaicLogger.getLogger().trace(
					"AmqpProxy - Sent " + opName.toString() + " request [" + id
							+ "]...");
		} catch (IOException e) {
			for (IOperationCompletionHandler<V> handler : handlers) {
				handler.onFailure(e);
			}
			ExceptionTracer.traceDeferred(new ConnectionException(
					"Cannot send " + opName.toString() + " request to driver: "
							+ e.getMessage(), e));
		}
		return id;
	}

}
