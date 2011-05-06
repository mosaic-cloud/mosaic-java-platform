package mosaic.connector.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mosaic.connector.kvstore.MemcachedStoreConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.StoreOperation;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Proxy for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used by the {@link MemcachedStoreConnector}
 * to communicate with a memcached driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedProxy {
	private static final String DEFAULT_QUEUE_NAME = "memcached_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "memcached";

	private IConfiguration configuration;
	private Channel commChannel;
	private Connection connection;
	private String exchange;
	private String routingKey;
	private String connectorId;

	private ResponseHandlerMap handlerMap;
	private MemcachedConnectorReactor responseReactor;

	/**
	 * Creates a proxy for memcached key-value distributed storage systems.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 */
	public MemcachedProxy(IConfiguration config) {
		this.configuration = config;
		connectorId = UUID.randomUUID().toString(); // FIXME this should be
													// replaced

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
		if (!amqpServerUser.equals("")) {
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

			// start also the response reactor for this proxy
			handlerMap = new ResponseHandlerMap();
			responseReactor = new MemcachedConnectorReactor(configuration,
					connectorId, handlerMap);
			Thread reactorThread = new Thread(responseReactor);
			reactorThread.start();
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

	public static MemcachedProxy create(IConfiguration config) {
		return new MemcachedProxy(config);
	}

	/**
	 * Destroys the proxy, freeing up any allocated resources.
	 */
	public void destroy() {
		// close connection
		try {
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
		}
		responseReactor.destroy();
	}

	public MemcachedConnectorReactor getResponseReactor() {
		return responseReactor;
	}

	public synchronized void set(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.SET, key, exp, data, handlers);
	}

	public synchronized void add(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.ADD, key, exp, data, handlers);
	}

	public synchronized void replace(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.REPLACE, key, exp, data, handlers);
	}

	public synchronized void append(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.APPEND, key, 0, data, handlers);
	}

	public synchronized void prepend(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.PREPEND, key, 0, data, handlers);
	}

	public synchronized void cas(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		sendStoreMessage(OperationNames.CAS, key, 0, data, handlers);
	}

	public synchronized void get(String key,
			List<IOperationCompletionHandler<Object>> handlers) {
		List<String> keys = new ArrayList<String>();
		keys.add(key);
		sendGetMessage(OperationNames.GET, keys, handlers);
	}

	public synchronized void getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers) {
		sendGetMessage(OperationNames.GET_BULK, keys, handlers);
	}

	public synchronized void delete(String key,
			List<IOperationCompletionHandler<Boolean>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, connectorId);

		MosaicLogger.getLogger().trace(
				"Sending " + OperationNames.DELETE.toString() + " request ["
						+ id + "]...");

		try {
			// store token and completion handlers
			this.handlerMap.addHandlers(id, handlers);

			DeleteOperation op = new DeleteOperation();
			op.put(0, token);
			op.put(1, OperationNames.DELETE);
			op.put(2, key);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			commChannel.basicPublish(exchange, routingKey, null, message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendStoreMessage(OperationNames operation, String key,
			int exp, Object data, List<IOperationCompletionHandler<Boolean>> handlers) {
		byte[] dataBytes;
		byte[] message;
		String id;
		ByteBuffer buff;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, connectorId);
		MosaicLogger.getLogger().trace(
				"Sending " + operation.toString() + " request [" + id + "]...");
		try {
			// store token and completion handlers
			this.handlerMap.addHandlers(id, handlers);

			dataBytes = SerDesUtils.toBytes(data);
			buff = ByteBuffer.wrap(dataBytes);
			StoreOperation op = new StoreOperation();
			op.put(0, token);
			op.put(1, operation);
			op.put(2, key);
			op.put(3, exp);
			op.put(4, buff);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			commChannel.basicPublish(exchange, routingKey, null, message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <T extends Object> void sendGetMessage(OperationNames operation, List<String> keys,
			List<IOperationCompletionHandler<T>> handlers) {
		byte[] message;
		String id;

		// build token
		id = UUID.randomUUID().toString();
		CompletionToken token = new CompletionToken();
		token.put(0, id);
		token.put(1, connectorId);

		MosaicLogger.getLogger().trace(
				"Sending " + operation.toString() + " request [" + id + "]...");

		try {
			// store token and completion handlers
			this.handlerMap.addHandlers(id, handlers);

			GetOperation op = new GetOperation();
			op.put(0, token);
			op.put(1, operation);
			op.put(2, keys);
			Operation enclosingOperation = new Operation();
			enclosingOperation.put(0, op);

			// send request
			message = SerDesUtils.serializeWithSchema(enclosingOperation);
			commChannel.basicPublish(exchange, routingKey, null, message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
