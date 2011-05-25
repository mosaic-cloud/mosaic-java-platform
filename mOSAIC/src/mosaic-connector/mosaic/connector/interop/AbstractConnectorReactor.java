package mosaic.connector.interop;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Dummy base class for connector reactors.
 * <p>
 * Note: This will probably be replaced when the real interoperability layer
 * will be integrated.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractConnectorReactor implements Runnable {
	private ResponseHandlerMap dispatcher;
	private boolean isAlive;
	private CountDownLatch cleanupSignal;

	private Channel commChannel;
	private Connection connection;
	private QueueingConsumer consumer;
	private String exchange;
	private String queueName;

	/**
	 * Creates the reactor for the connector proxy.
	 * <p>
	 * Note: The response reactor will start only after the
	 * {@link AbstractConnectorReactor#setDispatcher(ResponseHandlerMap)} is
	 * called.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param defaultExchange
	 *            the default exchange to be used by the reactor (in case one is
	 *            not given in the configuration data)
	 * @param defaultQueue
	 *            default queue to be used by the reactor (in case one is not
	 *            given in the configuration data)
	 */
	protected AbstractConnectorReactor(IConfiguration config,
			String bindingKey, String defaultExchange, String defaultQueue) {
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
				"interop.resp.amqp.exchange", String.class, defaultExchange);
		queueName = ConfigUtils.resolveParameter(config,
				"interop.resp.amqp.rountingkey", String.class, defaultQueue);

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
						"The Memcached proxy cannot connect to the driver: "
								+ e1.getMessage(), e1));
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
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The proxy cannot close connection to the driver: "
							+ e.getMessage(), e));
		} catch (InterruptedException e) {
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The proxy cannot close connection to the driver: "
							+ e.getMessage(), e));
		}
	}

	@Override
	public void run() {
		while (this.isAlive) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				processResponse(delivery.getBody());

				commChannel.basicAck(delivery.getEnvelope().getDeliveryTag(),
						false);
			} catch (IOException e) {
				ExceptionTracer.traceRethrown(e);
			} catch (ShutdownSignalException e) {
				ExceptionTracer.traceRethrown(e);
			} catch (InterruptedException e) {
				ExceptionTracer.traceRethrown(e);
			}
		}
		this.workDone();
	}

	/**
	 * Returns the dispatcher which holds the mappings between the requests and
	 * the handlers for their responses.
	 * 
	 * @return the dispatcher
	 */
	protected ResponseHandlerMap getDispatcher() {
		return this.dispatcher;
	}

	/**
	 * Waits for the next message and returns its contents.
	 * 
	 * @return the contents of the next message
	 */
	protected byte[] getNextMessage() {
		byte[] message = null;
		try {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			message = delivery.getBody();

			commChannel
					.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(e);
		} catch (ShutdownSignalException e) {
			ExceptionTracer.traceRethrown(e);
		} catch (InterruptedException e) {
			ExceptionTracer.traceRethrown(e);
		}
		return message;
	}

	/**
	 * Signals that the connector finished its work and the reactor can now be
	 * destroyed.
	 */
	protected void workDone() {
		this.cleanupSignal.countDown();
	}

	/**
	 * Process a received response for a previous submitted request.
	 * 
	 * @param message
	 *            the contents of the received message
	 * @throws IOException
	 */
	protected abstract void processResponse(byte[] message) throws IOException;

	/**
	 * If <code>true</code> then this rector is in the process of being
	 * destroyed and the reactor should finish its current work and shutdown.
	 * 
	 * @return <code>true</code> if this rector is in the process of being
	 *         destroyed
	 */
	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * Sets the dispatcher. The response reactor will start only after this
	 * method is called.
	 * 
	 * @param dispatcher
	 *            the dispatcher ( the map with handlers for response
	 *            processing)
	 */
	public void setDispatcher(ResponseHandlerMap dispatcher) {
		this.dispatcher = dispatcher;
		isAlive = true;
		cleanupSignal = new CountDownLatch(1);
	}

}
