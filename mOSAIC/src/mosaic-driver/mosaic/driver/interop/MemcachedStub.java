package mosaic.driver.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.kvstore.MemcachedDriver;
import mosaic.driver.kvstore.MemcachedOperations;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.StoreOperation;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Stub for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used for communicating with a memcached
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStub implements Runnable {
	private static final String DEFAULT_QUEUE_NAME = "memcached_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "";

	private IConfiguration configuration;
	private Channel commChannel;
	private Connection connection;
	private QueueingConsumer consumer;
	private String exchange;
	private String routingKey;
	private MemcachedResponseTransmitter transmitter;
	private MemcachedDriver driver;

	private boolean isAlive;
	private CountDownLatch cleanupSignal;

	public MemcachedStub(IConfiguration config) {
		super();
		this.configuration = config;

		// read connection details from the configuration
		String amqpServerHost = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.host", String.class,
				ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.port", Integer.class,
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.user", String.class,
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.passwd", String.class,
				ConnectionFactory.DEFAULT_PASS);
		exchange = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.exchange", String.class,
				DEFAULT_EXCHANGE_NAME);
		routingKey = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.rountingkey", String.class,
				DEFAULT_QUEUE_NAME);

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(amqpServerHost);
		factory.setPort(amqpServerPort);
		if (!amqpServerUser.isEmpty()) {
			factory.setUsername(amqpServerUser);
			factory.setPassword(amqpServerPasswd);
		}

		try {
			// create communication channel
			connection = factory.newConnection();
			commChannel = connection.createChannel();

			// create exchange and queue
			commChannel.exchangeDeclare(exchange, "direct", true);
			// commChannel.queueDeclare(routingKey, true, false, false, null);
			String queueName = commChannel.queueDeclare().getQueue();
			commChannel.queueBind(queueName, exchange, routingKey);

			// create consumer
			consumer = new QueueingConsumer(commChannel);
			commChannel.basicConsume(queueName, false, consumer);

			transmitter = new MemcachedResponseTransmitter(configuration);
			driver = MemcachedDriver.create(configuration);
			isAlive = true;
			cleanupSignal = new CountDownLatch(1);
		} catch (IOException e) {
			e.printStackTrace();
			// close connections
			try {
				if (commChannel != null && commChannel.isOpen()) {
					commChannel.close();
				}
				if (connection != null && connection.isOpen()) {
					connection.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				ExceptionTracer.traceRethrown(new ConnectionException(
						"The Memcached proxy cannot connect to the driver: "
								+ e1.getMessage()));
			}
		}
	}

	public static MemcachedStub create(IConfiguration config) {
		return new MemcachedStub(config);
	}

	/**
	 * Destroys this stub.
	 */
	public void destroy() {
		isAlive = false;

		// close connection
		try {
			cleanupSignal.await();
			if (commChannel != null && commChannel.isOpen()) {
				commChannel.close();
			}
			if (connection != null && connection.isOpen()) {
				connection.close();
			}
			System.out.println("stub closed connection");
		} catch (IOException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The Memcached proxy cannot close connection to the driver: "
							+ e.getMessage()));
		} catch (InterruptedException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The Memcached proxy cannot close connection to the driver: "
							+ e.getMessage()));
		}
		driver.destroy();
		transmitter.destroy();
	}

	@Override
	public void run() {
		while (isAlive) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				startOperation(delivery.getBody());
				commChannel.basicAck(delivery.getEnvelope().getDeliveryTag(),
						false);
			} catch (IOException e) {
				e.printStackTrace();
				ExceptionTracer.traceRethrown(e);
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
				ExceptionTracer.traceRethrown(e);
			} catch (InterruptedException e) {
				e.printStackTrace();
				ExceptionTracer.traceRethrown(e);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				ExceptionTracer.traceRethrown(e);
			}
		}
		cleanupSignal.countDown();
	}

	private void startOperation(byte[] message) throws IOException,
			ClassNotFoundException {
		CompletionToken token;
		OperationNames opName;
		String key;

		Operation op = new Operation();
		op = SerDesUtils.deserializeWithSchema(message, op);
		Object ob = op.get(0);

		if (ob instanceof StoreOperation) {
			StoreOperation sop = (StoreOperation) ob;
			token = (CompletionToken) sop.get(0);
			opName = (OperationNames) sop.get(1);
			key = ((CharSequence) sop.get(2)).toString();
			int exp = (Integer) sop.get(3);
			ByteBuffer dataBytes = (ByteBuffer) sop.get(4);
			Object data = SerDesUtils.toObject(dataBytes.array());

			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString()
							+ " - request id: " + token.get(0) + " client id: "
							+ token.get(1));

			// execute operation
			IResult<Boolean> resultStore = null;
			DriverOperationFinishedHandler storeCallback = new DriverOperationFinishedHandler(
					token);
			MemcachedOperations operation = MemcachedOperations.ADD;
			switch (opName) {
			case ADD:
				resultStore = driver.invokeAddOperation(key, exp, data,
						storeCallback);
				break;
			case APPEND:
				resultStore = driver.invokeAppendOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.APPEND;
				break;
			case CAS:
				resultStore = driver.invokeCASOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.CAS;
				break;
			case PREPEND:
				resultStore = driver.invokePrependOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.PREPEND;
				break;
			case REPLACE:
				resultStore = driver.invokeReplaceOperation(key, exp, data,
						storeCallback);
				operation = MemcachedOperations.REPLACE;
				break;
			case SET:
				resultStore = driver.invokeSetOperation(key, exp, data,
						storeCallback);
				operation = MemcachedOperations.SET;
				break;
			default:
				MosaicLogger.getLogger()
						.error("Unknown memcached store message: "
								+ opName.toString());
				break;
			}
			storeCallback.setDetails(operation, resultStore);
		} else if (ob instanceof DeleteOperation) {
			DeleteOperation dop = (DeleteOperation) ob;
			token = (CompletionToken) dop.get(0);
			opName = (OperationNames) dop.get(1);
			key = ((CharSequence) dop.get(2)).toString();
			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: "
							+ token.get(0) + " key: " + key);

			if (opName.equals(OperationNames.DELETE)) {
				DriverOperationFinishedHandler delCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Boolean> resultDelete = driver.invokeDeleteOperation(
						key, delCallback);
				delCallback
						.setDetails(MemcachedOperations.DELETE, resultDelete);

			} else {
				MosaicLogger.getLogger().error(
						"Unknown memcached delete message: "
								+ opName.toString());
			}
		} else if (ob instanceof GetOperation) {
			GetOperation gop = (GetOperation) ob;
			token = (CompletionToken) gop.get(0);
			opName = (OperationNames) gop.get(1);
			@SuppressWarnings("unchecked")
			List<CharSequence> keys = (List<CharSequence>) gop.get(2);

			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: "
							+ token.get(0));

			switch (opName) {
			case GET:
				DriverOperationFinishedHandler getCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Object> resultGet = driver.invokeGetOperation(
						keys.get(0).toString(), getCallback);
				getCallback.setDetails(MemcachedOperations.GET, resultGet);
				break;
			case GET_BULK:
				List<String> strKeys = new ArrayList<String>();
				for (CharSequence kcs : keys) {
					strKeys.add(kcs.toString());
				}
				DriverOperationFinishedHandler getBCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Map<String, Object>> resultGetBulk = driver
						.invokeGetBulkOperation(strKeys, getBCallback);
				getBCallback.setDetails(MemcachedOperations.GET_BULK,
						resultGetBulk);
				break;
			default:
				MosaicLogger.getLogger().error(
						"Unknown memcached get message: " + opName.toString());
				break;
			}
		}
	}

	final class DriverOperationFinishedHandler implements
			IOperationCompletionHandler {
		private IResult<?> result;
		private MemcachedOperations operation;
		private final CompletionToken complToken;
		private CountDownLatch signal;

		public DriverOperationFinishedHandler(CompletionToken complToken) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
		}

		public void setDetails(MemcachedOperations op, IResult<?> result) {
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
			MemcachedStub.this.driver.removePendingOperation(result);

			if (operation.equals(MemcachedOperations.GET)) {
				Map<String, Object> resMap = new HashMap<String, Object>();
				resMap.put("dummy", response);
				transmitter.sendResponse(complToken, operation, resMap, false);
			} else {
				transmitter
						.sendResponse(complToken, operation, response, false);
			}

		}

		@Override
		public void onFailure(Throwable error) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			MemcachedStub.this.driver.removePendingOperation(result);
			// result is error
			transmitter.sendResponse(complToken, operation, error.getMessage(),
					true);
		}
	}

}
