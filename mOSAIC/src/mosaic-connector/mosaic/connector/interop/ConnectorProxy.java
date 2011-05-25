package mosaic.connector.interop;

import java.io.IOException;
import java.util.List;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Dummy base class for connector proxys.
 * <p>
 * Note: This will probably be replaced when the real interoperability layer
 * will be integrated.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ConnectorProxy {
	private IConfiguration configuration;
	private Channel commChannel;
	private Connection connection;
	private String exchange;
	private String routingKey;
	private String connectorId;

	private ResponseHandlerMap handlerMap;
	private AbstractConnectorReactor responseReactor;

	/**
	 * Creates a proxy for a resource.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param defaultExchange
	 *            the default exchange to be used by the proxy (in case one is
	 *            not given in the configuration data)
	 * @param defaultQueue
	 *            default queue to be used by the proxy (in case one is not
	 *            given in the configuration data)
	 * @param reactor
	 *            the response reactor
	 */
	public ConnectorProxy(IConfiguration config, String connectorId,
			String defaultExchange, String defaultQueue,
			AbstractConnectorReactor reactor) {
		this.configuration = config;
		this.connectorId = connectorId;

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
				"interop.req.amqp.exchange", String.class, defaultExchange);
		routingKey = ConfigUtils.resolveParameter(config,
				"interop.req.amqp.rountingkey", String.class, defaultQueue);

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
			responseReactor = reactor;
			responseReactor.setDispatcher(handlerMap);
			Thread reactorThread = new Thread(responseReactor);
			reactorThread.start();
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
				ExceptionTracer.traceRethrown(new ConnectionException(
						"The proxy cannot connect to the driver: "
								+ e1.getMessage(), e1));
			}
		}
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
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The proxy cannot close connection to the driver: "
							+ e.getMessage(), e));
		}
		responseReactor.destroy();
	}

	/**
	 * Sends a request to the driver.
	 * 
	 * @param request
	 *            the request
	 * @throws IOException
	 */
	protected void sendRequest(byte[] request) throws IOException {
		commChannel.basicPublish(exchange, routingKey, null, request);
	}

	/**
	 * Returns the response reactor for the connector proxy.
	 * 
	 * @param <T>
	 *            the type of the response reactor
	 * @param reactorClass
	 *            the class of the response Freactor
	 * @return the response reactor
	 */
	public <T extends AbstractConnectorReactor> T getResponseReactor(
			Class<T> reactorClass) {
		return reactorClass.cast(responseReactor);
	}

	/**
	 * Returns the unique ID of the connector's proxy.
	 * 
	 * @return the ID of the connector's proxy
	 */
	protected String getConnectorId() {
		return connectorId;
	}

	/**
	 * Registers response handlers for a request.
	 * 
	 * @param <T>
	 *            the type of the result of the request
	 * @param requestId
	 *            the identifier for the request
	 * @param handlers
	 *            the list of the response handlers
	 */
	protected <T extends Object> void registerHandlers(String requestId,
			List<IOperationCompletionHandler<T>> handlers) {
		this.handlerMap.addHandlers(requestId, handlers);
	}
}
