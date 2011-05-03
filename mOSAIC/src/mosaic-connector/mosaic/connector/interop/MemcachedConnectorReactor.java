package mosaic.connector.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.MemcachedError;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.OperationResponse;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * Memcached connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedConnectorReactor implements Runnable {
	private static final String DEFAULT_QUEUE_NAME = "memcached_responses";
	private static final String DEFAULT_EXCHANGE_NAME = "memcached";

	private ResponseHandlerMap mcDispatcher;
	private boolean isAlive;
	private CountDownLatch cleanupSignal;

	private Channel commChannel;
	private Connection connection;
	private QueueingConsumer consumer;
	private String exchange;
	private String queueName;

	/**
	 * Creates the reactor for the Memcached connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param handlerMap
	 *            the map with handlers for response processing
	 */
	public MemcachedConnectorReactor(IConfiguration config, String bindingKey,
			ResponseHandlerMap handlerMap) {
		super();

		// read connection details from the configuration
		String amqpServerHost = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.host", String.class,
				ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.port", Integer.class,
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.user", String.class,
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.passwd", String.class,
				ConnectionFactory.DEFAULT_PASS);
		exchange = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.exchange", String.class,
				DEFAULT_EXCHANGE_NAME);
		queueName = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.rountingkey", String.class,
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
			// commChannel.queueDeclare(queueName, true, false, false, null);
			String anonQueue = commChannel.queueDeclare().getQueue();
			commChannel.queueBind(anonQueue, exchange, bindingKey);

			// create consumer
			consumer = new QueueingConsumer(commChannel);
			commChannel.basicConsume(anonQueue, false, consumer);

			mcDispatcher = handlerMap;
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

	/**
	 * Destroys this reactor.
	 */
	public void destroy() {
		// close connection
		isAlive = false;
		try {
			cleanupSignal.await();
			// commChannel.basicCancel(queueName);
			if (commChannel != null && commChannel.isOpen()) {
				commChannel.close();
			}
			if (connection != null && connection.isOpen()) {
				connection.close();
			}
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

		System.out.println("reactor destroyed");
	}

	@Override
	public void run() {
		while (isAlive) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				processResponse(delivery.getBody());

				commChannel.basicAck(delivery.getEnvelope().getDeliveryTag(),
						false);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ShutdownSignalException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		cleanupSignal.countDown();
	}

	private void processResponse(byte[] message) throws IOException {
		OperationResponse response = new OperationResponse();
		response = SerDesUtils.deserializeWithSchema(message, response);
		CompletionToken token = (CompletionToken) response.get(0);
		OperationNames op = (OperationNames) response.get(1);
		boolean isError = (Boolean) response.get(2);
		String id = ((CharSequence) token.get(0)).toString();

		List<IOperationCompletionHandler> handlers = this.mcDispatcher
				.removeRequestHandlers(id);
		if (handlers == null) {
			MosaicLogger.getLogger().error(
					"No handler found for request token: " + id);
			return;
		}
		ByteBuffer buff;
		Object data;

		if (isError) {
			MemcachedError error = (MemcachedError) response.get(3);
			for (IOperationCompletionHandler handler : handlers) {
				handler.onFailure(new Exception(((CharSequence) error.get(0))
						.toString()));
			}
			return;
		}

		switch (op) {
		case ADD:
		case SET:
		case APPEND:
		case PREPEND:
		case CAS:
		case REPLACE:
		case DELETE:
			Boolean resultB = (Boolean) response.get(3);
			for (IOperationCompletionHandler handler : handlers) {
				handler.onSuccess(resultB);
			}
			break;
		case GET:
			@SuppressWarnings("unchecked")
			Map<CharSequence, ByteBuffer> resultO = (Map<CharSequence, ByteBuffer>) response
					.get(3);
			buff = resultO.values().toArray(new ByteBuffer[0])[0];

			try {
				data = null;
				if (buff != null)
					data = SerDesUtils.toObject(buff.array());
				for (IOperationCompletionHandler handler : handlers) {
					handler.onSuccess(data);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			break;
		case GET_BULK:
			@SuppressWarnings("unchecked")
			Map<CharSequence, ByteBuffer> resultM = (Map<CharSequence, ByteBuffer>) response
					.get(3);
			Map<String, Object> resMap = new HashMap<String, Object>();
			try {
				for (Map.Entry<CharSequence, ByteBuffer> entry : resultM
						.entrySet()) {
					buff = entry.getValue();
					data = SerDesUtils.toObject(buff.array());
					resMap.put(entry.getKey().toString(), data);
				}
				for (IOperationCompletionHandler handler : handlers) {
					handler.onSuccess(resMap);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}
}
