package mosaic.driver.interop;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.driver.ConfigProperties;
import mosaic.driver.IResourceDriver;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Dummy base class for driver stubs.
 * <p>
 * Note: This will probably be replaced when the real interoperability layer
 * will be integrated.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractDriverStub implements Runnable {
	private IConfiguration configuration;
	private Channel commChannel;
	private Connection connection;
	private QueueingConsumer consumer;
	private String exchange;
	private String routingKey;
	private ResponseTransmitter transmitter;
	private IResourceDriver driver;

	protected boolean isAlive;
	protected CountDownLatch cleanupSignal;

	/**
	 * Builds a driver stub.
	 * 
	 * @param config
	 *            configuration data for the driver and its stub
	 * @param defaultExchange
	 *            the default exchange to be used by the stub (in case one is
	 *            not given in the configuration data)
	 * @param defaultQueue
	 *            default queue to be used by the stub (in case one is not given
	 *            in the configuration data)
	 * @param transmitter
	 *            the transmitter which will serialize and send responses back
	 *            to the connector
	 * @param driver
	 *            the driver which will handle requests received by the stub
	 */
	protected AbstractDriverStub(IConfiguration config, String defaultExchange,
			String defaultQueue, ResponseTransmitter transmitter,
			IResourceDriver driver) {
		super();
		this.configuration = config;

		// read connection details from the configuration
		String amqpServerHost = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.0"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.1"), Integer.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.2"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.3"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_PASS);
		exchange = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.4"), String.class, defaultExchange); //$NON-NLS-1$
		routingKey = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AbstractDriverStub.5"), String.class, defaultQueue); //$NON-NLS-1$

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
			commChannel.exchangeDeclare(exchange, "direct", true); //$NON-NLS-1$
			// commChannel.queueDeclare(routingKey, true, false, false, null);
			String queueName = commChannel.queueDeclare().getQueue();
			commChannel.queueBind(queueName, exchange, routingKey);

			// create consumer
			consumer = new QueueingConsumer(commChannel);
			commChannel.basicConsume(queueName, false, consumer);

			this.transmitter = transmitter;
			this.driver = driver;
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
						"The proxy cannot connect to the driver: " //$NON-NLS-1$
								+ e1.getMessage()));
			}
		}
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
		} catch (IOException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The proxy cannot close connection to the driver: " //$NON-NLS-1$
							+ e.getMessage()));
		} catch (InterruptedException e) {
			e.printStackTrace();
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The proxy cannot close connection to the driver: " //$NON-NLS-1$
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
				ExceptionTracer.traceRethrown(e);
			} catch (ShutdownSignalException e) {
				ExceptionTracer.traceRethrown(e);
			} catch (InterruptedException e) {
				ExceptionTracer.traceRethrown(e);
			} catch (ClassNotFoundException e) {
				ExceptionTracer.traceRethrown(e);
			}
		}
		cleanupSignal.countDown();
	}

	/**
	 * Returns the response transmitter used by the stub.
	 * 
	 * @param <T>
	 *            the type of the transmitter
	 * @param transClass
	 *            the class object of the transmitter
	 * @return the transmitter
	 */
	protected <T extends ResponseTransmitter> T getResponseTransmitter(
			Class<T> transClass) {
		return transClass.cast(this.transmitter);
	}

	/**
	 * Returns the driver used by the stub.
	 * 
	 * @param <T>
	 *            the type of the driver
	 * @param transClass
	 *            the class object of the driver
	 * @return the driver
	 */
	protected <T extends IResourceDriver> T getDriver(Class<T> driverClass) {
		return driverClass.cast(this.driver);
	}

	/**
	 * Deserializes a message received by the stub and starts the operation
	 * requested in the message.
	 * 
	 * @param message
	 *            the received message
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected abstract void startOperation(byte[] message) throws IOException,
			ClassNotFoundException;
}