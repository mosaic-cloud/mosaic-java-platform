package mosaic.driver.interop;

import java.io.IOException;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.driver.ConfigProperties;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Base class for driver response transmitter.
 * <p>
 * Note: This is a dummy for the real interoperability layer.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseTransmitter {
	private static final String DEFAULT_EXCHANGE_NAME = ""; //$NON-NLS-1$

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
	public ResponseTransmitter(IConfiguration config) {
		super();
		// read connection details from the configuration
		String amqpServerHost = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("ResponseTransmitter.1"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("ResponseTransmitter.2"), Integer.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("ResponseTransmitter.3"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("ResponseTransmitter.4"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_PASS);
		exchange = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("ResponseTransmitter.5"), String.class, //$NON-NLS-1$
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
			commChannel.exchangeDeclare(exchange, "direct", true); //$NON-NLS-1$
			// commChannel.queueDeclare(queueName, true, false, false, null);
			// commChannel.queueBind(queueName, exchange, "");
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(e);
			// close connections
			try {
				if (commChannel != null && commChannel.isOpen()) {
					commChannel.close();
				}
				if (connection != null && connection.isOpen()) {
					connection.close();
				}
			} catch (IOException e1) {
				ExceptionTracer.traceRethrown(e1);
			}
		}
	}

	/**
	 * Destroys the transmitter.
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
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The Memcached proxy cannot close connection to the driver: " //$NON-NLS-1$
							+ e.getMessage(), e));
		}
	}

	/**
	 * Sends result to the connector's proxy.
	 * 
	 * @param routingKey
	 *            the routing key for the response message
	 * @param message
	 *            the message
	 */
	protected void publishResponse(String routingKey, byte[] message) {
		try {
			commChannel.basicPublish(exchange, routingKey, null, message);
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(e);
		}
	}

}