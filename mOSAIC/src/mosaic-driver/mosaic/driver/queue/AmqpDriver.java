package mosaic.driver.queue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import mosaic.connector.queue.AmqpInboundMessage;
import mosaic.connector.queue.AmqpOutboundMessage;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
import mosaic.core.ops.IResult;
import mosaic.driver.AbstractResourceDriver;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.FlowListener;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Driver class for the AMQP-based management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpDriver extends AbstractResourceDriver {

	private boolean connected;
	private IConfiguration configuration;
	private AmqpOperationFactory opFactory;

	private Connection connection;
	private List<Channel> channels;
	private Channel defaultChannel;
	private FlowCallback flowCallback;
	private ReturnCallback returnCallback;

	/**
	 * Creates a new driver.
	 * 
	 * @param configuration
	 *            configuration data required for starting the driver
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 */
	private AmqpDriver(IConfiguration configuration, int noThreads) {
		super(noThreads);
		this.configuration = configuration;
		this.connected = false;

		this.opFactory = new AmqpOperationFactory();

		this.returnCallback = new ReturnCallback();
		this.flowCallback = new FlowCallback();
		this.connection = null;
		this.channels = null;
		this.defaultChannel = null;
	}

	/**
	 * Returns an AMQP driver.
	 * 
	 * @param configuration
	 *            configuration data required for starting the driver
	 * @return an AMQP driver
	 */
	public static AmqpDriver create(IConfiguration configuration) {
		int noThreads = ConfigUtils.resolveParameter(configuration,
				"amqp.driver_threads", Integer.class, 1);
		return new AmqpDriver(configuration, noThreads);
	}

	@Override
	public void destroy() {
		super.destroy();

		// close any existing connection
		if (this.connected) {
			try {
				for (Channel channel : AmqpDriver.this.channels)
					channel.close();
				this.connection.close();
				this.connected = false;
			} catch (IOException e) {
				MosaicLogger.getLogger().error(
						"AMQP cannot close connection with server.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Opens a new AMQP connection. If one already exists then the current one
	 * is closed and a new one is created.
	 * 
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if the connection was established
	 */
	public synchronized IResult<Boolean> openConnection(
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.OPEN_CONNECTION);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Closes the connection with the AMQP queue system. It also closes all
	 * channels.
	 * 
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if the connection was closed
	 */
	public synchronized IResult<Boolean> closeConnection(
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.CLOSE_CONNECTION);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Declares an exchange and creates a channel for it.
	 * 
	 * @param name
	 *            the name of the exchange
	 * @param type
	 *            the exchange type
	 * @param durable
	 *            <code>true</code> if we are declaring a durable exchange (the
	 *            exchange will survive a server restart)
	 * @param autoDelete
	 *            <code>true</code> if the server should delete the exchange
	 *            when it is no longer in use
	 * @param passive
	 *            <code>true</code> if we declare an exchange passively; that
	 *            is, check if the named exchange exists
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if the exchange declaration succeeded
	 */
	public IResult<Boolean> declareExchange(
			String name,
			AmqpExchangeType type,
			boolean durable,
			boolean autoDelete,
			boolean passive,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.DECLARE_EXCHANGE, name, type,
						durable, autoDelete, passive);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Declare a queue.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param exclusive
	 *            <code>true</code> if we are declaring an exclusive queue
	 *            (restricted to this connection)
	 * @param durable
	 *            <code>true</code> if we are declaring a durable queue (the
	 *            queue will survive a server restart)
	 * @param autoDelete
	 *            <code>true</code> if we are declaring an autodelete queue
	 *            (server will delete it when no longer in use)
	 * @param passive
	 *            <code>true</code> if we declare a queue passively; i.e., check
	 *            if it exists
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if the queue declaration succeeded
	 */
	public IResult<Boolean> declareQueue(
			String queue,
			boolean exclusive,
			boolean durable,
			boolean autoDelete,
			boolean passive,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.DECLARE_QUEUE, queue, exclusive,
						durable, autoDelete, passive);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Bind a queue to an exchange, with no extra arguments.
	 * 
	 * @param exchange
	 *            the name of the queue
	 * @param queue
	 *            the name of the exchange
	 * @param routingKey
	 *            the routing key to use for the binding
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if the queue bind succeeded
	 */
	public IResult<Boolean> bindQueue(
			String exchange,
			String queue,
			String routingKey,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.BIND_QUEUE, exchange, queue,
						routingKey);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Publishes a message.
	 * 
	 * @param message
	 *            the message, message properties and destination data
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if message was published successfully
	 */
	public IResult<Boolean> basicPublish(
			AmqpOutboundMessage message,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.PUBLISH, message);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Start a message consumer.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param consumer
	 *            a client-generated consumer tag to establish context
	 * @param exclusive
	 *            <code>true</code> if this is an exclusive consumer
	 * @param autoAck
	 *            <code>true</code> if the server should consider messages
	 *            acknowledged once delivered; false if the server should expect
	 *            explicit acknowledgments
	 * @param extra
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return the client-generated consumer tag to establish context
	 */
	public IResult<String> basicConsume(
			String queue,
			String consumer,
			boolean exclusive,
			boolean autoAck,
			Object extra,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<String> op = (GenericOperation<String>) this.opFactory
				.getOperation(AmqpOperations.CONSUME, queue, consumer,
						exclusive, autoAck, extra);

		IResult<String> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Acknowledge one or several received messages.
	 * 
	 * @param delivery
	 *            the tag received with the messages
	 * @param multiple
	 *            <code>true</code> to acknowledge all messages up to and
	 *            including the supplied delivery tag; <code>false</code> to
	 *            acknowledge just the supplied delivery tag.
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if messages were acknowledged successfully
	 */
	public IResult<Boolean> basicAck(
			long delivery,
			boolean multiple,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.ACK, delivery, multiple);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Retrieve a message from a queue.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param autoAck
	 *            <code>true</code> if the server should consider messages
	 *            acknowledged once delivered; <code>false</code> if the server
	 *            should expect explicit acknowledgments
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if message was retrieved successfully
	 */
	public IResult<Boolean> basicGet(
			String queue,
			boolean autoAck,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.GET, queue, autoAck);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	/**
	 * Cancels a consumer.
	 * 
	 * @param consumer
	 *            a client- or server-generated consumer tag to establish
	 *            context
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return <code>true</code> if consumer was canceled
	 */
	public IResult<Boolean> basicCancel(
			String consumer,
			@SuppressWarnings("rawtypes") IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.CANCEL, consumer);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	private Channel getDefaultChannel() {
		if (this.defaultChannel == null)
			this.defaultChannel = this.openChannel();
		return this.defaultChannel;
	}

	private Channel openChannel() {
		Channel channel = null;
		synchronized (this) {
			try {
				if (this.connected) {
					channel = this.connection.createChannel();
					channel.setDefaultConsumer(null);
					channel.setReturnListener(this.returnCallback);
					channel.setFlowListener(this.flowCallback);
					this.channels.add(channel);
				}
			} catch (IOException e) {
				e.printStackTrace();
				MosaicLogger.getLogger().error(e.getMessage());
			}
		}

		return channel;
	}

	private <T extends Object> IResult<T> startOperation(
			GenericOperation<T> op, IOperationCompletionHandler complHandler) {
		IResult<T> iResult = new GenericResult<T>(op);
		op.setHandler(complHandler);
		super.addPendingOperation(iResult);

		super.submitOperation(op.getOperation());
		return iResult;
	}

	/**
	 * Handler for channel flow events.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	private final class FlowCallback implements FlowListener {

		@Override
		public void handleFlow(boolean active) {
		}
	}

	/**
	 * Message consumer class which will receive notifications and messages from
	 * a queue by subscription.
	 * <p>
	 * Note: all methods of this class are invoked inside the Connection's
	 * thread. This means they a) should be non-blocking and generally do little
	 * work, b) must not call Channel or Connection methods, or a deadlock will
	 * ensue.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	private final class ConsumerCallback implements Consumer {
		private Object extra;

		ConsumerCallback(Object extra) {
			super();
			this.extra = extra;
		}

		@Override
		public final void handleCancelOk(String consumer) {
			MosaicLogger.getLogger().trace(
					"Received CANCEL Ok callback for consumer " + consumer
							+ ".");
			;
			// TODO
		}

		@Override
		public final void handleConsumeOk(String consumer) {
			MosaicLogger.getLogger().trace(
					"Received CONSUME Ok callback for consumer " + consumer
							+ ".");
			// TODO
		}

		@Override
		public final void handleDelivery(String consumer, Envelope envelope,
				AMQP.BasicProperties properties, byte[] data) {
			AmqpInboundMessage message = new AmqpInboundMessage(consumer,
					envelope.getDeliveryTag(), envelope.getExchange(),
					envelope.getRoutingKey(), data,
					properties.getDeliveryMode() == 2 ? true : false,
					properties.getReplyTo(), properties.getContentEncoding(),
					properties.getContentType(), properties.getCorrelationId(),
					properties.getMessageId());
			MosaicLogger.getLogger().trace(
					"Received delivery " + message.getDelivery());
			// TODO
		}

		@Override
		public final void handleRecoverOk() {
			// TODO
		}

		@Override
		public final void handleShutdownSignal(String consumer,
				ShutdownSignalException signal) {
			// TODO
		}
	}

	/**
	 * Listener to be called in order to be notified of failed deliveries when
	 * basicPublish is called with "mandatory" or "immediate" flags set.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	private final class ReturnCallback implements ReturnListener {
		@Override
		public void handleBasicReturn(int replyCode, String replyMessage,
				String exchange, String routingKey, BasicProperties properties,
				byte[] data) {
			AmqpInboundMessage message = new AmqpInboundMessage(null, -1,
					exchange, routingKey, data,
					properties.getDeliveryMode() == 2 ? true : false,
					properties.getReplyTo(), properties.getContentEncoding(),
					properties.getContentType(), properties.getCorrelationId(),
					properties.getMessageId());
			MosaicLogger.getLogger().trace(
					"Received RETURN callback for " + message.getDelivery());
			// TODO
		}
	}

	/**
	 * Factory class which builds the asynchronous calls for the operations
	 * defined for the AMQP protocol.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class AmqpOperationFactory implements IOperationFactory {

		AmqpOperationFactory() {
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.core.IOperationFactory#getOperation(mosaic.core.IOperationType
		 * , java.lang.Object[])
		 */
		@Override
		public IOperation<?> getOperation(IOperationType type,
				Object... parameters) {
			IOperation<?> operation = null;
			if (!(type instanceof AmqpOperations)) {
				throw new IllegalArgumentException("Unsupported operation: "
						+ type.toString());
			}
			AmqpOperations mType = (AmqpOperations) type;
			final String queue;
			final String exchange;
			final boolean durable;
			final boolean autoDelete;
			final boolean passive;
			final boolean autoAck;
			final boolean exclusive;
			final String consumer;

			switch (mType) {
			case OPEN_CONNECTION:
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								if (AmqpDriver.this.connected) {
									for (Channel channel : AmqpDriver.this.channels)
										channel.close();
									AmqpDriver.this.connection.close();
									AmqpDriver.this.connected = false;
								}
								String amqpServerHost = ConfigUtils
										.resolveParameter(
												AmqpDriver.this.configuration,
												"amqp.host", String.class,
												ConnectionFactory.DEFAULT_HOST);
								int amqpServerPort = ConfigUtils
										.resolveParameter(
												AmqpDriver.this.configuration,
												"amqp.port",
												Integer.class,
												ConnectionFactory.DEFAULT_AMQP_PORT);
								String amqpServerUser = ConfigUtils
										.resolveParameter(
												AmqpDriver.this.configuration,
												"amqp.user", String.class,
												ConnectionFactory.DEFAULT_USER);
								String amqpServerPasswd = ConfigUtils
										.resolveParameter(
												AmqpDriver.this.configuration,
												"amqp.passwd", String.class,
												ConnectionFactory.DEFAULT_PASS);
								String amqpVirtualHost = ConfigUtils
										.resolveParameter(
												AmqpDriver.this.configuration,
												"amqp.virtual_host",
												String.class,
												ConnectionFactory.DEFAULT_VHOST);

								ConnectionFactory factory = new ConnectionFactory();
								factory.setHost(amqpServerHost);
								factory.setPort(amqpServerPort);
								factory.setVirtualHost(amqpVirtualHost);
								if (!amqpServerUser.isEmpty()) {
									factory.setUsername(amqpServerUser);
									factory.setPassword(amqpServerPasswd);
								}

								try {
									AmqpDriver.this.connection = factory
											.newConnection();
									AmqpDriver.this.channels = new LinkedList<Channel>();
									succeeded = true;
								} catch (IOException e) {
									e.printStackTrace();
									MosaicLogger.getLogger().error(
											e.getMessage());
									AmqpDriver.this.connection = null;
								}
								return succeeded;
							}

						});
				break;
			case CLOSE_CONNECTION:
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								if (AmqpDriver.this.connected) {
									try {
										for (Channel channel : AmqpDriver.this.channels)
											channel.close();
										AmqpDriver.this.connection.close();
										AmqpDriver.this.connected = false;
										succeeded = true;
									} catch (IOException e) {
										e.printStackTrace();
										MosaicLogger.getLogger().error(
												e.getMessage());
									}
								}
								return succeeded;
							}

						});
				break;
			case DECLARE_EXCHANGE:
				exchange = (String) parameters[0];
				final AmqpExchangeType eType = (AmqpExchangeType) parameters[1];
				durable = (Boolean) parameters[2];
				autoDelete = (Boolean) parameters[3];
				passive = (Boolean) parameters[4];

				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;

								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										AMQP.Exchange.DeclareOk outcome = null;
										if (passive)
											outcome = channel
													.exchangeDeclarePassive(exchange);
										else
											outcome = channel.exchangeDeclare(
													exchange,
													eType.getAmqpName(),
													durable, autoDelete, null);
										succeeded = (outcome != null);
									}
								}
								return succeeded;
							}

						});
				break;
			case DECLARE_QUEUE:
				queue = (String) parameters[0];
				exclusive = (Boolean) parameters[1];
				durable = (Boolean) parameters[2];
				autoDelete = (Boolean) parameters[3];
				passive = (Boolean) parameters[4];

				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										AMQP.Queue.DeclareOk outcome = null;
										if (passive)
											outcome = channel
													.queueDeclarePassive(queue);
										else
											outcome = channel.queueDeclare(
													queue, durable, exclusive,
													autoDelete, null);
										succeeded = (outcome != null);
									}
								}
								return succeeded;
							}

						});
				break;
			case BIND_QUEUE:
				exchange = (String) parameters[0];
				queue = (String) parameters[1];
				final String routingKey = (String) parameters[2];

				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									try {
										if (channel != null) {
											AMQP.Queue.BindOk outcome = channel
													.queueBind(queue, exchange,
															routingKey, null);
											succeeded = (outcome != null);
										}
									} catch (IOException e) {
										e.printStackTrace();
										MosaicLogger.getLogger().error(
												e.getMessage());
									}
								}
								return succeeded;
							}

						});
				break;
			case PUBLISH:
				final AmqpOutboundMessage message = (AmqpOutboundMessage) parameters[0];
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										AMQP.BasicProperties properties = new AMQP.BasicProperties(
												message.getContentType(),
												message.getContentEncoding(),
												null, message.isDurable() ? 2
														: 1, 0, message
														.getCorrelation(),
												message.getCallback(), null,
												message.getIdentifier(), null,
												null, null, null, null);
										channel.basicPublish(
												message.getExchange(),
												message.getRoutingKey(),
												properties, message.getData());
										succeeded = true;
									}
								}
								return succeeded;
							}

						});
				break;
			case CONSUME:
				queue = (String) parameters[0];
				consumer = (String) parameters[1];
				exclusive = (Boolean) parameters[2];
				autoAck = (Boolean) parameters[3];
				final Object extra = parameters[4];

				operation = new GenericOperation<String>(
						new Callable<String>() {

							@Override
							public String call() throws Exception {
								String consumerTag = null;
								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										consumerTag = channel.basicConsume(
												queue, autoAck, consumer, true,
												exclusive, null,
												new ConsumerCallback(extra));
									}
								}
								return consumerTag;
							}

						});
				break;
			case GET:
				queue = (String) parameters[0];
				autoAck = (Boolean) parameters[1];
				operation = new GenericOperation<AmqpInboundMessage>(
						new Callable<AmqpInboundMessage>() {

							@Override
							public AmqpInboundMessage call() throws Exception {
								AmqpInboundMessage message = null;
								synchronized (AmqpDriver.this) {

									final Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										GetResponse outcome = null;
										try {
											outcome = channel.basicGet(queue,
													autoAck);
											if (outcome != null) {
												final Envelope envelope = outcome
														.getEnvelope();
												final AMQP.BasicProperties properties = outcome
														.getProps();
												message = new AmqpInboundMessage(
														null,
														envelope.getDeliveryTag(),
														envelope.getExchange(),
														envelope.getRoutingKey(),
														outcome.getBody(),
														properties
																.getDeliveryMode() == 2 ? true
																: false,
														properties.getReplyTo(),
														properties
																.getContentEncoding(),
														properties
																.getContentType(),
														properties
																.getCorrelationId(),
														properties
																.getMessageId());
											}
										} catch (IOException e) {
											e.printStackTrace();
											MosaicLogger.getLogger().error(
													e.getMessage());
										}
									}
								}
								return message;
							}

						});
				break;
			case ACK:
				final long delivery = (Long) parameters[0];
				final boolean multiple = (Boolean) parameters[1];
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								synchronized (AmqpDriver.this) {
									final Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										try {
											channel.basicAck(delivery, multiple);
											succeeded = true;
										} catch (IOException e) {
											e.printStackTrace();
											MosaicLogger.getLogger().error(
													e.getMessage());
										}
									}
								}
								return succeeded;
							}

						});
				break;
			case CANCEL:
				consumer = (String) parameters[0];
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								boolean succeeded = false;
								synchronized (AmqpDriver.this) {
									final Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										try {
											channel.basicCancel(consumer);
											succeeded = true;
										} catch (IOException e) {
											e.printStackTrace();
											MosaicLogger.getLogger().error(
													e.getMessage());
										}
									}
								}
								return succeeded;
							}

						});
				break;
			default:
				throw new UnsupportedOperationException(
						"Unsupported operation: " + mType.toString());
			}

			return operation;
		}
	}
}
