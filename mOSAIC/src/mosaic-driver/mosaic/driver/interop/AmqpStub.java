package mosaic.driver.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import mosaic.connector.queue.AmqpInboundMessage;
import mosaic.connector.queue.AmqpOutboundMessage;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.IResourceDriver;
import mosaic.driver.queue.AmqpDriver;
import mosaic.driver.queue.AmqpExchangeType;
import mosaic.driver.queue.AmqpOperations;
import mosaic.driver.queue.IAmqpConsumer;
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

/**
 * Stub for the driver for queuing systems implementing the AMQP protocol. This
 * is used for communicating with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpStub extends AbstractDriverStub implements Runnable {
	private static final String DEFAULT_QUEUE_NAME = "amqp_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "amqp";

	/**
	 * Creates a new stub for the AMQP driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @param transmitter
	 *            the transmitter object which will send responses to requests
	 *            submitted to this stub
	 * @param driver
	 *            the driver used for processing requests submitted to this stub
	 */
	private AmqpStub(IConfiguration config, ResponseTransmitter transmitter,
			IResourceDriver driver) {
		super(config, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME, transmitter,
				driver);
	}

	/**
	 * Returns a stub for the AMQP driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @return the AMQP driver stub
	 */
	public static AmqpStub create(IConfiguration config) {
		AmqpStub stub = null;
		AmqpResponseTransmitter transmitter = new AmqpResponseTransmitter(
				config);
		AmqpDriver driver = AmqpDriver.create(config);
		stub = new AmqpStub(config, transmitter, driver);
		return stub;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.driver.interop.AbstractDriverStub#startOperation(byte[])
	 */
	protected void startOperation(byte[] message) throws IOException,
			ClassNotFoundException {
		IResult<Boolean> resultBool;
		IResult<String> resultString;
		String queue;
		String exchange;
		boolean durable;
		boolean autoDelete;
		boolean passive;
		boolean autoAck;
		boolean exclusive;
		String consumer;
		String routingKey;
		ByteBuffer dataBytes;
		AmqpDriver driver = super.getDriver(AmqpDriver.class);

		Operation op = new Operation();
		op = SerDesUtils.deserializeWithSchema(message, op);
		CompletionToken token = (CompletionToken) op.get(0);
		OperationNames opName = (OperationNames) op.get(1);
		Object ob = op.get(2);

		MosaicLogger.getLogger().trace(
				"Received request for " + opName.toString() + " - request id: "
						+ token.get(0) + " client id: " + token.get(1));

		// execute operation
		DriverOperationFinishedHandler complHandler = new DriverOperationFinishedHandler(
				token);
		switch (opName) {
		case OPEN_CONNECTION:
			resultBool = driver.openConnection(complHandler);
			complHandler.setDetails(AmqpOperations.OPEN_CONNECTION, resultBool);
			break;
		case CLOSE_CONNECTION:
			resultBool = driver.closeConnection(complHandler);
			complHandler
					.setDetails(AmqpOperations.CLOSE_CONNECTION, resultBool);
			break;
		case DECLARE_EXCHANGE:
			DeclareExchangeOperation deop = (DeclareExchangeOperation) ob;
			exchange = (String) deop.get(0);
			ExchangeType type = (ExchangeType) deop.get(1);
			durable = (Boolean) deop.get(2);
			autoDelete = (Boolean) deop.get(3);
			passive = (Boolean) deop.get(4);
			resultBool = driver.declareExchange(exchange,
					AmqpExchangeType.valueOf(type.toString().toUpperCase()),
					durable, autoDelete, passive, complHandler);
			complHandler
					.setDetails(AmqpOperations.DECLARE_EXCHANGE, resultBool);
			break;
		case DECLARE_QUEUE:
			DeclareQueueOperation dqop = (DeclareQueueOperation) ob;
			queue = (String) dqop.get(0);
			exclusive = (Boolean) dqop.get(1);
			durable = (Boolean) dqop.get(2);
			autoDelete = (Boolean) dqop.get(3);
			passive = (Boolean) dqop.get(4);
			resultBool = driver.declareQueue(queue, exclusive, durable,
					autoDelete, passive, complHandler);
			complHandler.setDetails(AmqpOperations.DECLARE_QUEUE, resultBool);
			break;
		case BIND_QUEUE:
			BindQueueOperation bqop = (BindQueueOperation) ob;
			exchange = (String) bqop.get(0);
			queue = (String) bqop.get(1);
			routingKey = (String) bqop.get(2);
			resultBool = driver.bindQueue(exchange, queue, routingKey,
					complHandler);
			complHandler.setDetails(AmqpOperations.BIND_QUEUE, resultBool);
			break;
		case PUBLISH:
			PublishOperation pop = (PublishOperation) ob;
			String callback = (String) pop.get(0);
			String contentEncoding = (String) pop.get(1);
			String contentType = (String) pop.get(2);
			String correlation = (String) pop.get(3);
			dataBytes = (ByteBuffer) pop.get(4);
			durable = (Boolean) pop.get(5);
			exchange = (String) pop.get(6);
			String identifier = (String) pop.get(7);
			boolean immediate = (Boolean) pop.get(8);
			boolean mandatory = (Boolean) pop.get(9);
			routingKey = (String) pop.get(10);
			AmqpOutboundMessage mssg = new AmqpOutboundMessage(exchange,
					routingKey, dataBytes.array(), mandatory, immediate,
					durable, callback, contentEncoding, contentType,
					correlation, identifier);
			resultBool = driver.basicPublish(mssg, complHandler);
			complHandler.setDetails(AmqpOperations.PUBLISH, resultBool);
			break;
		case CONSUME:
			ConsumeOperation cop = (ConsumeOperation) ob;
			queue = (String) cop.get(0);
			consumer = (String) cop.get(1);
			exclusive = (Boolean) cop.get(2);
			autoAck = (Boolean) cop.get(3);
			dataBytes = (ByteBuffer) cop.get(4);
			Object extra = SerDesUtils.toObject(dataBytes.array());
			IAmqpConsumer consumeCallback = new ConsumerHandler(
					(String) token.get(1));
			resultString = driver.basicConsume(queue, consumer, exclusive,
					autoAck, extra, consumeCallback, complHandler);
			complHandler.setDetails(AmqpOperations.CONSUME, resultString);
			break;
		case GET:
			GetOperation gop = (GetOperation) ob;
			queue = (String) gop.get(0);
			autoAck = (Boolean) gop.get(1);
			resultBool = driver.basicGet(queue, autoAck, complHandler);
			complHandler.setDetails(AmqpOperations.GET, resultBool);
			break;
		case CANCEL:
			CancelOperation clop = (CancelOperation) ob;
			consumer = (String) clop.get(0);
			resultBool = driver.basicCancel(consumer, complHandler);
			complHandler.setDetails(AmqpOperations.CANCEL, resultBool);
			break;
		case ACK:
			AckOperation aop = (AckOperation) ob;
			long delivery = (Long) aop.get(0);
			boolean multiple = (Boolean) aop.get(1);
			resultBool = driver.basicAck(delivery, multiple, complHandler);
			complHandler.setDetails(AmqpOperations.ACK, resultBool);
			break;
		default:
			MosaicLogger.getLogger().error(
					"Unknown amqp message: " + opName.toString());
			break;
		}
	}

	/**
	 * Handler for processing responses of the requests submitted to the stub.
	 * This will basically call the transmitter associated with the stub.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class DriverOperationFinishedHandler implements
			IOperationCompletionHandler {
		private IResult<?> result;
		private AmqpOperations operation;
		private final CompletionToken complToken;
		private CountDownLatch signal;
		private AmqpDriver driver;
		private AmqpResponseTransmitter transmitter;

		public DriverOperationFinishedHandler(CompletionToken complToken) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = AmqpStub.this.getDriver(AmqpDriver.class);
			this.transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
		}

		public void setDetails(AmqpOperations op, IResult<?> result) {
			this.operation = op;
			this.result = result;
			this.signal.countDown();
		}

		@Override
		public void onSuccess(Object response) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.driver.removePendingOperation(result);

			transmitter.sendResponse(complToken, operation, response, false);

		}

		@Override
		public void onFailure(Throwable error) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.driver.removePendingOperation(result);
			// result is error
			transmitter.sendResponse(complToken, operation, error.getMessage(),
					true);
		}
	}

	final class ConsumerHandler implements IAmqpConsumer {
		private String callerId;

		public ConsumerHandler(String callerId) {
			super();
			this.callerId = callerId;
		}

		/* (non-Javadoc)
		 * @see mosaic.driver.queue.IAmqpConsumer#handleConsumeOk(java.lang.String)
		 */
		@Override
		public void handleConsumeOk(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendConsumeOk(callerId, consumerTag);

		}

		/* (non-Javadoc)
		 * @see mosaic.driver.queue.IAmqpConsumer#handleCancelOk(java.lang.String)
		 */
		@Override
		public void handleCancelOk(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendCancelOk(callerId, consumerTag);

		}

		/* (non-Javadoc)
		 * @see mosaic.driver.queue.IAmqpConsumer#handleDelivery(mosaic.connector.queue.AmqpInboundMessage)
		 */
		@Override
		public void handleDelivery(AmqpInboundMessage message) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendDelivery(callerId, message);

		}

		/* (non-Javadoc)
		 * @see mosaic.driver.queue.IAmqpConsumer#handleShutdown(java.lang.String, java.lang.String)
		 */
		@Override
		public void handleShutdown(String consumerTag, String errorMessage) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendShutdownSignal(callerId, consumerTag, errorMessage);

		}
	}
}
