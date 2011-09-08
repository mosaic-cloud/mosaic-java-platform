package mosaic.driver.interop.queue.amqp;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
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
import mosaic.interop.amqp.AmqpMessage;
import mosaic.interop.amqp.AmqpSession;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.amqp.AmqpPayloads;
import mosaic.interop.idl.amqp.AmqpPayloads.Ack;
import mosaic.interop.idl.amqp.AmqpPayloads.BindQueueRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.CancelRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.ConsumeRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest.ExchangeType;
import mosaic.interop.idl.amqp.AmqpPayloads.DeclareQueueRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.GetRequest;
import mosaic.interop.idl.amqp.AmqpPayloads.PublishRequest;

import com.google.common.base.Preconditions;
import com.rabbitmq.client.ConnectionFactory;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Stub for the driver for queuing systems implementing the AMQP protocol. This
 * is used for communicating with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpStub extends AbstractDriverStub {

	private static AmqpStub stub;

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
	 * @param commChannel
	 *            the channel for communicating with connectors
	 */
	private AmqpStub(IConfiguration config, ResponseTransmitter transmitter,
			IResourceDriver driver, ZeroMqChannel commChannel) {
		super(config, transmitter, driver, commChannel);
	}

	/**
	 * Returns a stub for the AMQP driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @param channel
	 *            the channel for communicating with connectors
	 * @return the AMQP driver stub
	 */
	public static AmqpStub create(IConfiguration config, ZeroMqChannel channel) {
		synchronized (AbstractDriverStub.lock) {
			if (AmqpStub.stub == null) {
				AmqpResponseTransmitter transmitter = new AmqpResponseTransmitter(
						config);
				AmqpDriver driver = AmqpDriver.create(config);
				AmqpStub.stub = new AmqpStub(config, transmitter, driver,
						channel);
				incDriverReference(AmqpStub.stub);
				channel.accept(AmqpSession.DRIVER, AmqpStub.stub);
				MosaicLogger.getLogger().trace("AmqpStub: created new stub."); //$NON-NLS-1$
			} else {
				MosaicLogger.getLogger().trace("AmqpStub: use existing stub."); //$NON-NLS-1$
				incDriverReference(AmqpStub.stub);
			}
		}
		return AmqpStub.stub;
	}

	@Override
	public void destroy() {
		synchronized (AbstractDriverStub.lock) {
			decDriverReference(this);
		}
		super.destroy();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void startOperation(Message message, Session session)
			throws IOException, ClassNotFoundException {
		Preconditions
				.checkArgument(message.specification instanceof AmqpMessage);
		AmqpMessage amqpMessage = (AmqpMessage) message.specification;

		CompletionToken token = null;
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
		byte[] dataBytes;
		AmqpDriver driver = super.getDriver(AmqpDriver.class);

		switch (amqpMessage) {
		case ACCESS:
			MosaicLogger.getLogger().trace("Received initiation message"); //$NON-NLS-1$
			break;
		case ABORTED:
			MosaicLogger.getLogger().trace("Received termination message"); //$NON-NLS-1$
			break;
		case DECL_EXCHANGE_REQUEST:
			AmqpPayloads.DeclareExchangeRequest declExchange = (DeclareExchangeRequest) message.payload;
			token = declExchange.getToken();
			exchange = declExchange.getExchange();
			ExchangeType type = declExchange.getType();
			durable = declExchange.getDurable();
			autoDelete = declExchange.getAutoDelete();
			passive = declExchange.getPassive();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for DECLARE EXCHANGE " //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler exchHandler = new DriverOperationFinishedHandler(
					token, session);

			resultBool = driver.declareExchange(exchange,
					AmqpExchangeType.valueOf(type.toString().toUpperCase()),
					durable, autoDelete, passive, exchHandler);
			exchHandler.setDetails(AmqpOperations.DECLARE_EXCHANGE, resultBool);
			break;
		case DECL_QUEUE_REQUEST:
			AmqpPayloads.DeclareQueueRequest declQueue = (DeclareQueueRequest) message.payload;
			token = declQueue.getToken();
			queue = declQueue.getQueue();
			exclusive = declQueue.getExclusive();
			durable = declQueue.getDurable();
			autoDelete = declQueue.getAutoDelete();
			passive = declQueue.getPassive();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for DECLARE QUEUE" //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler queueHandler = new DriverOperationFinishedHandler(
					token, session);

			resultBool = driver.declareQueue(queue, exclusive, durable,
					autoDelete, passive, queueHandler);
			queueHandler.setDetails(AmqpOperations.DECLARE_QUEUE, resultBool);
			break;
		case BIND_QUEUE_REQUEST:
			AmqpPayloads.BindQueueRequest bindQueue = (BindQueueRequest) message.payload;
			token = bindQueue.getToken();
			exchange = bindQueue.getExchange();
			queue = bindQueue.getQueue();
			routingKey = bindQueue.getRoutingKey();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for BIND QUEUE " //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler bindHandler = new DriverOperationFinishedHandler(
					token, session);

			resultBool = driver.bindQueue(exchange, queue, routingKey,
					bindHandler);
			bindHandler.setDetails(AmqpOperations.BIND_QUEUE, resultBool);
			break;
		case PUBLISH_REQUEST:
			AmqpPayloads.PublishRequest publish = (PublishRequest) message.payload;
			token = publish.getToken();
			dataBytes = publish.getData().toByteArray();
			durable = publish.getDurable();
			exchange = publish.getExchange();
			boolean immediate = publish.getImmediate();
			boolean mandatory = publish.getMandatory();
			routingKey = publish.getRoutingKey();
			AmqpOutboundMessage mssg = new AmqpOutboundMessage(exchange,
					routingKey, dataBytes, mandatory, immediate, durable,
					publish.getContentType());

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for PUBLISH " //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler pubHandler = new DriverOperationFinishedHandler(
					token, session);

			resultBool = driver.basicPublish(mssg, pubHandler);
			pubHandler.setDetails(AmqpOperations.PUBLISH, resultBool);
			break;
		case CONSUME_REQUEST:
			AmqpPayloads.ConsumeRequest cop = (ConsumeRequest) message.payload;
			token = cop.getToken();
			queue = cop.getQueue();
			consumer = cop.getConsumer();
			exclusive = cop.getExclusive();
			autoAck = cop.getAutoAck();
			dataBytes = cop.getExtra().toByteArray();
			Object extra = SerDesUtils.toObject(dataBytes);

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for CONSUME " //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler consHandler = new DriverOperationFinishedHandler(
					token, session);

			IAmqpConsumer consumeCallback = new ConsumerHandler(session);
			resultString = driver.basicConsume(queue, consumer, exclusive,
					autoAck, extra, consumeCallback, consHandler);
			consHandler.setDetails(AmqpOperations.CONSUME, resultString);
			break;
		case GET_REQUEST:
			AmqpPayloads.GetRequest gop = (GetRequest) message.payload;
			token = gop.getToken();
			queue = gop.getQueue();
			autoAck = gop.getAutoAck();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for GET " + " - request id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.getMessageId() + " client id: " //$NON-NLS-1$
							+ token.getClientId());

			// execute operation
			DriverOperationFinishedHandler getHandler = new DriverOperationFinishedHandler(
					token, session);

			resultBool = driver.basicGet(queue, autoAck, getHandler);
			getHandler.setDetails(AmqpOperations.GET, resultBool);
			break;
		case CANCEL_REQUEST:
			AmqpPayloads.CancelRequest clop = (CancelRequest) message.payload;
			token = clop.getToken();
			consumer = clop.getConsumer();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received request for CANCEL " //$NON-NLS-1$
							+ " - request id: " + token.getMessageId() //$NON-NLS-1$
							+ " client id: " + token.getClientId()); //$NON-NLS-1$

			// execute operation
			DriverOperationFinishedHandler cancelHandler = new DriverOperationFinishedHandler(
					token, session);
			resultBool = driver.basicCancel(consumer, cancelHandler);
			cancelHandler.setDetails(AmqpOperations.CANCEL, resultBool);
			break;
		case ACK:
			AmqpPayloads.Ack aop = (Ack) message.payload;
			token = aop.getToken();
			long delivery = aop.getDelivery();
			boolean multiple = aop.getMultiple();

			MosaicLogger.getLogger().trace(
					"AmqpStub - Received  ACK " + " - request id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.getMessageId() + " client id: " //$NON-NLS-1$
							+ token.getClientId());

			// execute operation
			DriverOperationFinishedHandler ackHandler = new DriverOperationFinishedHandler(
					token, session);
			resultBool = driver.basicAck(delivery, multiple, ackHandler);
			ackHandler.setDetails(AmqpOperations.ACK, resultBool);
			break;
		default:
			DriverOperationFinishedHandler errHandler = new DriverOperationFinishedHandler(
					token, session);
			driver.handleUnsupportedOperationError(amqpMessage.toString(),
					errHandler);
			MosaicLogger.getLogger().error(
					"Unknown amqp message: " + amqpMessage.toString()); //$NON-NLS-1$
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
				"", String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_HOST);
		int resourcePort = ConfigUtils.resolveParameter(config,
				"", Integer.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				"", String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				"", String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_PASS);

		DriverConnectionData cData = null;
		if (amqpServerUser.equals(ConnectionFactory.DEFAULT_USER)
				&& amqpServerPasswd.equals(ConnectionFactory.DEFAULT_PASS)) {
			cData = new DriverConnectionData(resourceHost, resourcePort, "AMQP"); //$NON-NLS-1$
		} else {
			cData = new DriverConnectionData(resourceHost, resourcePort,
					"AMQP", amqpServerUser, amqpServerPasswd); //$NON-NLS-1$
		}
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
		private Session session;

		public DriverOperationFinishedHandler(CompletionToken complToken,
				Session session) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = AmqpStub.this.getDriver(AmqpDriver.class);
			this.transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			this.session = session;
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
				ExceptionTracer.traceDeferred(e);
			}
			this.driver.removePendingOperation(this.result);
			this.transmitter.sendResponse(this.session, this.complToken,
					this.operation, response, false);

		}

		@Override
		public void onFailure(Throwable error) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceDeferred(e);
			}
			this.driver.removePendingOperation(this.result);
			// result is error
			this.transmitter.sendResponse(this.session, this.complToken,
					this.operation, error.getMessage(), true);
		}
	}

	final class ConsumerHandler implements IAmqpConsumer {

		private Session session;

		public ConsumerHandler(Session session) {
			super();
			this.session = session;
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
			transmitter.sendConsumeOk(this.session, consumerTag);
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
			transmitter.sendCancelOk(this.session, consumerTag);

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
			transmitter.sendDelivery(this.session, message);

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
			transmitter.sendShutdownSignal(this.session, consumerTag,
					errorMessage);

		}

		@Override
		public void handleCancel(String consumerTag) {
			AmqpResponseTransmitter transmitter = AmqpStub.this
					.getResponseTransmitter(AmqpResponseTransmitter.class);
			transmitter.sendCancel(this.session, consumerTag);

		}
	}

}
