package mosaic.driver.interop.queue.amqp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.ConfigProperties;
import mosaic.driver.IResourceDriver;
import mosaic.driver.interop.AbstractDriverStub;
import mosaic.driver.interop.DriverConnectionData;
import mosaic.driver.interop.ResponseTransmitter;
import mosaic.driver.queue.amqp.AmqpDriver;
import mosaic.driver.queue.amqp.AmqpExchangeType;
import mosaic.driver.queue.amqp.AmqpInboundMessage;
import mosaic.driver.queue.amqp.AmqpOperations;
import mosaic.driver.queue.amqp.AmqpOutboundMessage;
import mosaic.driver.queue.amqp.IAmqpConsumer;
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

import com.rabbitmq.client.ConnectionFactory;

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
	private static Map<DriverConnectionData, AmqpStub> stubs = new HashMap<DriverConnectionData, AmqpStub>();

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
		DriverConnectionData cData = AmqpStub.readConnectionData(config);
		AmqpStub stub = null;
		synchronized (AbstractDriverStub.lock) {
			stub = stubs.get(cData);
			if (stub == null) {
				MosaicLogger.getLogger().trace("AmqpStub: create new stub.");
				AmqpResponseTransmitter transmitter = new AmqpResponseTransmitter(
						config);
				AmqpDriver driver = AmqpDriver.create(config);
				stub = new AmqpStub(config, transmitter, driver);
				stubs.put(cData, stub);
				// FIXME this will be removed - the driver will be started from
				// somewhere else
				Thread driverThread = new Thread(stub);
				driverThread.start();
			} else
				MosaicLogger.getLogger().trace("AmqpStub: use existing stub.");
		}
		return stub;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.driver.interop.AbstractDriverStub#startOperation(byte[])
	 */
	@SuppressWarnings("unchecked")
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
				"AmqpStub - Received request for " + opName.toString()
						+ " - request id: " + token.get(0) + " client id: "
						+ token.get(1));

		// execute operation
		DriverOperationFinishedHandler complHandler = new DriverOperationFinishedHandler(
				token);
		switch (opName) {
		case DECLARE_EXCHANGE:
			DeclareExchangeOperation deop = (DeclareExchangeOperation) ob;
			exchange = deop.get(0).toString();
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
			queue = dqop.get(0).toString();
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
			exchange = bqop.get(0).toString();
			queue = bqop.get(1).toString();
			routingKey = bqop.get(2).toString();
			resultBool = driver.bindQueue(exchange, queue, routingKey,
					complHandler);
			complHandler.setDetails(AmqpOperations.BIND_QUEUE, resultBool);
			break;
		case PUBLISH:
			PublishOperation pop = (PublishOperation) ob;
			// String callback = pop.get(0).toString();
			// String contentEncoding = pop.get(1).toString();
			// String contentType = pop.get(2).toString();
			// String correlation = pop.get(3).toString();
			dataBytes = (ByteBuffer) pop.get(0);
			durable = (Boolean) pop.get(1);
			exchange = pop.get(2).toString();
			// String identifier = pop.get(7).toString();
			boolean immediate = (Boolean) pop.get(3);
			boolean mandatory = (Boolean) pop.get(4);
			routingKey = pop.get(5).toString();
			AmqpOutboundMessage mssg = new AmqpOutboundMessage(exchange,
					routingKey, dataBytes.array(), mandatory, immediate,
					durable);
			resultBool = driver.basicPublish(mssg, complHandler);
			complHandler.setDetails(AmqpOperations.PUBLISH, resultBool);
			break;
		case CONSUME:
			ConsumeOperation cop = (ConsumeOperation) ob;
			queue = cop.get(0).toString();
			consumer = cop.get(1).toString();
			exclusive = (Boolean) cop.get(2);
			autoAck = (Boolean) cop.get(3);
			dataBytes = (ByteBuffer) cop.get(4);
			Object extra = SerDesUtils.toObject(dataBytes.array());
			IAmqpConsumer consumeCallback = new ConsumerHandler(token.get(1)
					.toString());
			resultString = driver.basicConsume(queue, consumer, exclusive,
					autoAck, extra, consumeCallback, complHandler);
			complHandler.setDetails(AmqpOperations.CONSUME, resultString);
			break;
		case GET:
			GetOperation gop = (GetOperation) ob;
			queue = gop.get(0).toString();
			autoAck = (Boolean) gop.get(1);
			resultBool = driver.basicGet(queue, autoAck, complHandler);
			complHandler.setDetails(AmqpOperations.GET, resultBool);
			break;
		case CANCEL:
			CancelOperation clop = (CancelOperation) ob;
			consumer = clop.get(0).toString();
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
			driver.handleUnsupportedOperationError(opName.name(), complHandler);
			MosaicLogger.getLogger().error(
					"Unknown amqp message: " + opName.toString());
			break;
		}
	}

	/**
	 * Reads resource connection data from the configuration data.
	 * 
	 * @param config
	 *            the configuration data
	 * @return resource connection data
	 */
	protected static DriverConnectionData readConnectionData(
			IConfiguration config) {
		String resourceHost = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AmqpDriver.1"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_HOST);
		int resourcePort = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AmqpDriver.2"), Integer.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AmqpDriver.3"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AmqpDriver.4"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_PASS);

		DriverConnectionData cData = null;
		if (amqpServerUser.equals(ConnectionFactory.DEFAULT_USER)
				&& amqpServerPasswd.equals(ConnectionFactory.DEFAULT_PASS))
			cData = new DriverConnectionData(resourceHost, resourcePort, "AMQP");
		else
			cData = new DriverConnectionData(resourceHost, resourcePort,
					"AMQP", amqpServerUser, amqpServerPasswd);
		return cData;
	}

	/**
	 * Handler for processing responses of the requests submitted to the stub.
	 * This will basically call the transmitter associated with the stub.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	@SuppressWarnings("rawtypes")
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.driver.queue.IAmqpConsumer#handleConsumeOk(java.lang.String)
		 */
		@Override
		public void handleConsumeOk(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendConsumeOk(callerId, consumerTag);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.driver.queue.IAmqpConsumer#handleCancelOk(java.lang.String)
		 */
		@Override
		public void handleCancelOk(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendCancelOk(callerId, consumerTag);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.driver.queue.IAmqpConsumer#handleDelivery(mosaic.connector
		 * .queue.AmqpInboundMessage)
		 */
		@Override
		public void handleDelivery(AmqpInboundMessage message) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendDelivery(callerId, message);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.driver.queue.IAmqpConsumer#handleShutdown(java.lang.String,
		 * java.lang.String)
		 */
		@Override
		public void handleShutdown(String consumerTag, String errorMessage) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendShutdownSignal(callerId, consumerTag, errorMessage);

		}

		@Override
		public void handleCancel(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendCancel(callerId, consumerTag);

		}
	}
}
