package mosaic.driver.interop;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
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
	private Thread runner;

	protected boolean isAlive;
	protected CountDownLatch cleanupSignal;

	protected static final Object lock = new Object();

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
		String amqpServerHost = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.0"), String.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.1"), Integer.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.2"), String.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.3"), String.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_PASS);
		exchange = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.4"), String.class, defaultExchange); //$NON-NLS-1$
		routingKey = ConfigUtils
				.resolveParameter(
						config,
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
			commChannel.exchangeDeclare(exchange, "direct", false, false, null); //$NON-NLS-1$
			// commChannel.queueDeclare(routingKey, true, false, false, null);

			String queueName = commChannel.queueDeclare("", false, true, true,
					null).getQueue();
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
				ExceptionTracer.traceDeferred(new ConnectionException(
						"The proxy cannot connect to the driver: " //$NON-NLS-1$
								+ e1.getMessage()));
			}
		}
	}

	/**
	 * Destroys this stub.
	 */
	public synchronized void destroy() {
		isAlive = false;

		// close connection
		try {
			this.runner.interrupt();
			cleanupSignal.await();
			if (commChannel != null && commChannel.isOpen()) {
				commChannel.close();
			}
			if (connection != null && connection.isOpen()) {
				connection.close();
			}
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(new ConnectionException(
					"The proxy cannot close connection to the driver: " //$NON-NLS-1$
							+ e.getMessage()));
		} catch (InterruptedException e) {
			ExceptionTracer.traceDeferred(new ConnectionException(
					"The proxy cannot close connection to the driver: " //$NON-NLS-1$
							+ e.getMessage()));
		}
		driver.destroy();
		transmitter.destroy();
		MosaicLogger.getLogger().trace("DriverStub destroyed.");
	}

	@Override
	public void run() {
		this.runner = Thread.currentThread();
		while (true) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				synchronized (this) {
					if (Thread.interrupted() && !this.isAlive)
						break;
					startOperation(delivery.getBody());
					commChannel.basicAck(delivery.getEnvelope()
							.getDeliveryTag(), false);
					if (!isAlive)
						break;
				}
			} catch (IOException e) {
				ExceptionTracer.traceDeferred(e);
			} catch (ShutdownSignalException e) {
				if (!isAlive)
					break;
				ExceptionTracer.traceDeferred(e);
			} catch (InterruptedException e) {
				if (!isAlive)
					break;
				ExceptionTracer.traceDeferred(e);
			} catch (ClassNotFoundException e) {
				ExceptionTracer.traceDeferred(e);
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
	 * Reads resource connection data from the configuration data.
	 * 
	 * @param config
	 *            the configuration data
	 * @return resource connection data
	 */
	protected static DriverConnectionData readConnectionData(
			IConfiguration config) {
		String amqpServerHost = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.0"), String.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("AbstractDriverStub.1"), Integer.class, //$NON-NLS-1$
						ConnectionFactory.DEFAULT_AMQP_PORT);
		DriverConnectionData cData = new DriverConnectionData(amqpServerHost,
				amqpServerPort);
		return cData;
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