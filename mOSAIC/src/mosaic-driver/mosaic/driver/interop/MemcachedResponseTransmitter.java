package mosaic.driver.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.kvstore.MemcachedOperations;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.MemcachedError;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.OperationResponse;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter {
	// private static final String DEFAULT_QUEUE_NAME = "memcached_responses";
	private static final String DEFAULT_EXCHANGE_NAME = "";

	private Channel commChannel;
	private Connection connection;
	private String exchange;

	// private String queueName;

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public MemcachedResponseTransmitter(IConfiguration config) {
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
		// queueName = ConfigUtils.resolveParameter(config,
		// "interop.resp.amqp.rountingkey", String.class,
		// DEFAULT_QUEUE_NAME);

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

			// create exchange
			commChannel.exchangeDeclare(exchange, "direct", true);
			// commChannel.queueDeclare(queueName, true, false, false, null);
			// commChannel.queueBind(queueName, exchange, "");
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
			}
		}
	}

	/**
	 * Destrys the transmitter.
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
			System.out.println("transmiter closed connection");
		} catch (IOException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The Memcached proxy cannot close connection to the driver: "
							+ e.getMessage()));
		}
	}

	/**
	 * Builds the result and sends it to the operation originator.
	 * 
	 * @param token
	 *            the token identifying the operation
	 * @param op
	 *            the identifier of the operation
	 * @param result
	 *            the result
	 * @param isError
	 *            <code>true</code> if the result is actual an error
	 */
	public void sendResponse(CompletionToken token, MemcachedOperations op,
			Object result, boolean isError) {
		byte[] dataBytes;
		byte[] message;
		String routingKey = ((CharSequence) token.get(1)).toString();

		OperationResponse response = new OperationResponse();
		try {
			response.put(0, token);
			response.put(1, convertOperationType(op));
			response.put(2, isError);
			if (!isError) {
				if (result instanceof Boolean) {
					response.put(3, (Boolean) result);
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> resMap = (Map<String, Object>) result;
					Map<CharSequence, ByteBuffer> sendMap = new HashMap<CharSequence, ByteBuffer>();
					for (Map.Entry<String, Object> entry : resMap.entrySet()) {
						dataBytes = SerDesUtils.toBytes(entry.getValue());
						ByteBuffer buff = ByteBuffer.wrap(dataBytes);
						sendMap.put(entry.getKey(), buff);
					}

					response.put(3, sendMap);
				}
			} else {
				MemcachedError error = new MemcachedError();
				error.put(0, (String) result);
				response.put(3, error);
			}
			// send response
			message = SerDesUtils.serializeWithSchema(response);
			commChannel.basicPublish(exchange, routingKey, null, message);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private OperationNames convertOperationType(MemcachedOperations op) {
		OperationNames cOp = null;
		switch (op) {
		case ADD:
			cOp = OperationNames.ADD;
			break;
		case APPEND:
			cOp = OperationNames.APPEND;
			break;
		case CAS:
			cOp = OperationNames.CAS;
			break;
		case DELETE:
			cOp = OperationNames.DELETE;
			break;
		case GET:
			cOp = OperationNames.GET;
			break;
		case GET_BULK:
			cOp = OperationNames.GET_BULK;
			break;
		case PREPEND:
			cOp = OperationNames.PREPEND;
			break;
		case REPLACE:
			cOp = OperationNames.REPLACE;
			break;
		case SET:
			cOp = OperationNames.SET;
			break;
		}
		return cOp;
	}
}
