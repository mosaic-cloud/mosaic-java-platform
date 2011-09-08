package mosaic.driver.queue.amqp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
import mosaic.core.ops.IResult;
import mosaic.driver.AbstractResourceDriver;
import mosaic.driver.ConfigProperties;

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
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Driver class for the AMQP-based management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpDriver extends AbstractResourceDriver {

	private boolean connected;
	IConfiguration configuration;
	private AmqpOperationFactory opFactory;

	private Connection connection;
	private List<Channel> channels;
	private Channel defaultChannel;
	private FlowCallback flowCallback;
	private ReturnCallback returnCallback;
	private ShutdownListener shutdownListener;
	private ConcurrentHashMap<String, IAmqpConsumer> consumers;
	private ExecutorService executor;

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
		this.shutdownListener = new ConnectionShutdownListener();
		this.connection = null;
		this.channels = null;
		this.defaultChannel = null;
		this.consumers = new ConcurrentHashMap<String, IAmqpConsumer>();
		this.executor = Executors.newFixedThreadPool(1);
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
				ConfigProperties.getString("AmqpDriver.0"), Integer.class, 1); //$NON-NLS-1$
		AmqpDriver driver = new AmqpDriver(configuration, noThreads);
		// open connection - moved to the stub
		driver.connectResource();
		if (!driver.connected) {
			driver = null;
		}
		return driver;
	}

	private void connectResource() {
		String amqpServerHost = ConfigUtils.resolveParameter(
				this.configuration,
				ConfigProperties.getString("AmqpDriver.1"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_HOST);
		int amqpServerPort = ConfigUtils.resolveParameter(this.configuration,
				ConfigProperties.getString("AmqpDriver.2"), //$NON-NLS-1$
				Integer.class, ConnectionFactory.DEFAULT_AMQP_PORT);
		String amqpServerUser = ConfigUtils.resolveParameter(
				this.configuration,
				ConfigProperties.getString("AmqpDriver.3"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_USER);
		String amqpServerPasswd = ConfigUtils.resolveParameter(
				this.configuration,
				ConfigProperties.getString("AmqpDriver.4"), String.class, //$NON-NLS-1$
				ConnectionFactory.DEFAULT_PASS);
		String amqpVirtualHost = ConfigUtils.resolveParameter(
				this.configuration, ConfigProperties.getString("AmqpDriver.5"), //$NON-NLS-1$
				String.class, ConnectionFactory.DEFAULT_VHOST);
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(amqpServerHost);
		factory.setPort(amqpServerPort);
		if (!amqpVirtualHost.equals("")) {
			factory.setVirtualHost(amqpVirtualHost);
		}
		if (!amqpServerUser.isEmpty()) {
			factory.setUsername(amqpServerUser);
			factory.setPassword(amqpServerPasswd);
		}

		try {
			this.connection = factory.newConnection();
			this.connection.addShutdownListener(this.shutdownListener);
			this.channels = new LinkedList<Channel>();
			this.connected = true;
			MosaicLogger.getLogger().debug(
					"AMQP driver connected to " + amqpServerHost + ":"
							+ amqpServerPort);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
			this.connection = null;
		}
	}

	@Override
	public synchronized void destroy() {
		super.destroy();

		// close any existing connection
		if (this.connected) {
			try {
				for (Channel channel : AmqpDriver.this.channels) {
					channel.close();
				}
				this.connection.close();
				this.connected = false;
			} catch (IOException e) {
				MosaicLogger.getLogger().error(
						"AMQP cannot close connection with server."); //$NON-NLS-1$
				ExceptionTracer.traceDeferred(e);
			}
		}
		MosaicLogger.getLogger().trace("AmqpDriver destroyed."); //$NON-NLS-1$
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
	public IResult<Boolean> declareExchange(String name, AmqpExchangeType type,
			boolean durable, boolean autoDelete, boolean passive,
			IOperationCompletionHandler<Boolean> complHandler) {
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
	public IResult<Boolean> declareQueue(String queue, boolean exclusive,
			boolean durable, boolean autoDelete, boolean passive,
			IOperationCompletionHandler<Boolean> complHandler) {
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
	public IResult<Boolean> bindQueue(String exchange, String queue,
			String routingKey, IOperationCompletionHandler<Boolean> complHandler) {
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
	public IResult<Boolean> basicPublish(AmqpOutboundMessage message,
			IOperationCompletionHandler<Boolean> complHandler) {
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
	 * @param consumeCallback
	 *            the consumer callback (this will called when the queuing
	 *            system will send Consume messages)
	 * @param complHandler
	 *            handlers to be called when the operation finishes
	 * @return the client-generated consumer tag to establish context
	 */
	public IResult<String> basicConsume(String queue, String consumer,
			boolean exclusive, boolean autoAck, Object extra,
			IAmqpConsumer consumeCallback,
			IOperationCompletionHandler<String> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<String> op = (GenericOperation<String>) this.opFactory
				.getOperation(AmqpOperations.CONSUME, queue, consumer,
						exclusive, autoAck, extra, consumeCallback);

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
	public IResult<Boolean> basicAck(long delivery, boolean multiple,
			IOperationCompletionHandler<Boolean> complHandler) {
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
	public IResult<Boolean> basicGet(String queue, boolean autoAck,
			IOperationCompletionHandler<Boolean> complHandler) {
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
	public IResult<Boolean> basicCancel(String consumer,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(AmqpOperations.CANCEL, consumer);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	private Channel getDefaultChannel() {
		if (this.defaultChannel == null) {
			this.defaultChannel = this.openChannel();
		}
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
				ExceptionTracer.traceDeferred(e);
			}
		}

		return channel;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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
		public final void handleCancelOk(final String consumer) {
			MosaicLogger
					.getLogger()
					.trace("AmqpDriver - Received CANCEL Ok callback for consumer " + consumer //$NON-NLS-1$
							+ "."); //$NON-NLS-1$
			;
			final IAmqpConsumer cancelCallback = AmqpDriver.this.consumers
					.remove(consumer);
			if (cancelCallback != null) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						cancelCallback.handleCancelOk(consumer);
					}
				};
				AmqpDriver.this.executor.execute(task);
			}
		}

		@Override
		public final void handleConsumeOk(final String consumer) {
			MosaicLogger
					.getLogger()
					.trace("AmqpDriver - Received CONSUME Ok callback for consumer " + consumer //$NON-NLS-1$
							+ "."); //$NON-NLS-1$
			final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers
					.get(consumer);
			if (consumeCallback != null) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						consumeCallback.handleConsumeOk(consumer);
					}
				};
				AmqpDriver.this.executor.execute(task);
			} else {
				MosaicLogger
						.getLogger()
						.error("AmqpDriver - no callback to handle CONSUME Ok message"); //$NON-NLS-1$
			}
		}

		@Override
		public final void handleDelivery(String consumer, Envelope envelope,
				AMQP.BasicProperties properties, byte[] data) {
			final AmqpInboundMessage message = new AmqpInboundMessage(consumer,
					envelope.getDeliveryTag(), envelope.getExchange(),
					envelope.getRoutingKey(), data,
					((properties.getDeliveryMode() != null) && (properties
							.getDeliveryMode() == 2)) ? true : false,
					properties.getContentType());
			final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers
					.get(consumer);
			if (consumeCallback != null) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						consumeCallback.handleDelivery(message);
					}
				};
				AmqpDriver.this.executor.execute(task);
			}
		}

		@Override
		public final void handleRecoverOk() {
		}

		@Override
		public final void handleShutdownSignal(final String consumer,
				final ShutdownSignalException signal) {
			MosaicLogger.getLogger().trace(
					"AmqpDriver - Received SHUTDOWN callback for consumer " //$NON-NLS-1$
							+ consumer + "."); //$NON-NLS-1$
			final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers
					.get(consumer);
			if (consumeCallback != null) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						consumeCallback.handleShutdown(consumer,
								signal.getMessage());
					}
				};
				AmqpDriver.this.executor.execute(task);
			}
		}

		@Override
		public void handleCancel(final String consumer) throws IOException {
			MosaicLogger
					.getLogger()
					.trace("AmqpDriver - Received CANCEL callback for consumer " + consumer //$NON-NLS-1$
							+ "."); //$NON-NLS-1$
			;
			final IAmqpConsumer cancelCallback = AmqpDriver.this.consumers
					.remove(consumer);
			if (cancelCallback != null) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						cancelCallback.handleCancel(consumer);
					}
				};
				AmqpDriver.this.executor.execute(task);
			}
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
		public void handleReturn(int replyCode, String replyMessage,
				String exchange, String routingKey, BasicProperties properties,
				byte[] data) throws IOException {
			AmqpInboundMessage message = new AmqpInboundMessage(null, -1,
					exchange, routingKey, data,
					properties.getDeliveryMode() == 2 ? true : false,
					properties.getReplyTo(), properties.getContentEncoding(),
					properties.getContentType(), properties.getCorrelationId(),
					properties.getMessageId());
			MosaicLogger
					.getLogger()
					.trace("AmqpDriver - Received RETURN callback for " + message.getDelivery()); //$NON-NLS-1$
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
		public IOperation<?> getOperation(final IOperationType type,
				Object... parameters) {
			IOperation<?> operation = null;
			if (!(type instanceof AmqpOperations)) {
				operation = new GenericOperation<Object>(
						new Callable<Object>() {

							@Override
							public Object call() throws Exception {
								throw new UnsupportedOperationException(
										"Unsupported operation: "
												+ type.toString());
							}

						});
				return operation;
			}

			final AmqpOperations mType = (AmqpOperations) type;
			final String queue;
			final String exchange;
			final boolean durable;
			final boolean autoDelete;
			final boolean passive;
			final boolean autoAck;
			final boolean exclusive;
			final String consumer;

			switch (mType) {
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
										if (passive) {
											outcome = channel
													.exchangeDeclarePassive(exchange);
										} else {
											outcome = channel.exchangeDeclare(
													exchange,
													eType.getAmqpName(),
													durable, autoDelete, null);
										}
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
										if (passive) {
											outcome = channel
													.queueDeclarePassive(queue);
										} else {
											outcome = channel.queueDeclare(
													queue, durable, exclusive,
													autoDelete, null);
										}
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

									try {
										Channel channel = AmqpDriver.this
												.getDefaultChannel();
										if (channel != null) {
											AMQP.Queue.BindOk outcome = channel
													.queueBind(queue, exchange,
															routingKey, null);
											succeeded = (outcome != null);
										}
									} catch (IOException e) {
										ExceptionTracer.traceDeferred(e);
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
				final IAmqpConsumer consumeCallback = (IAmqpConsumer) parameters[5];

				operation = new GenericOperation<String>(
						new Callable<String>() {

							@Override
							public String call() throws Exception {
								String consumerTag = null;
								synchronized (AmqpDriver.this) {
									Channel channel = AmqpDriver.this
											.getDefaultChannel();
									if (channel != null) {
										AmqpDriver.this.consumers.put(consumer,
												consumeCallback);
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
											ExceptionTracer.traceDeferred(e);
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
											ExceptionTracer.traceDeferred(e);
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
											ExceptionTracer.traceDeferred(e);
										}
									}
								}
								return succeeded;
							}

						});
				break;
			default:
				operation = new GenericOperation<Object>(
						new Callable<Object>() {

							@Override
							public Object call() throws Exception {
								throw new UnsupportedOperationException(
										"Unsupported operation: "
												+ mType.toString());
							}

						});
			}

			return operation;
		}

		@Override
		public void destroy() {

		}
	}

	/**
	 * Listener for connection shutdown signals.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class ConnectionShutdownListener implements ShutdownListener {

		private static final int DEFAULT_MAX_RECONNECTION_TRIES = 3;
		private static final long DEFAULT_MIN_RECONNECTION_TIME = 1000;

		private final int maxReconnectionTries;
		private final long minReconnectionTime;

		public ConnectionShutdownListener() {
			this.maxReconnectionTries = ConfigUtils.resolveParameter(
					AmqpDriver.this.configuration,
					ConfigProperties.getString("AmqpDriver.6"), Integer.class, //$NON-NLS-1$
					ConnectionShutdownListener.DEFAULT_MAX_RECONNECTION_TRIES);
			this.minReconnectionTime = ConfigUtils.resolveParameter(
					AmqpDriver.this.configuration,
					ConfigProperties.getString("AmqpDriver.7"), Long.class, //$NON-NLS-1$
					ConnectionShutdownListener.DEFAULT_MIN_RECONNECTION_TIME);
		}

		@Override
		public void shutdownCompleted(ShutdownSignalException arg0) {
			synchronized (AmqpDriver.this) {
				if (AmqpDriver.super.isDestroyed())
					return;
				MosaicLogger
						.getLogger()
						.trace("AMQP server closed connection with driver. Trying to reconnect..."); //$NON-NLS-1$
				AmqpDriver.this.connected = false;
				int tries = 0;
				while (!AmqpDriver.this.connected
						&& (tries < this.maxReconnectionTries)) {
					try {
						Thread.sleep(this.minReconnectionTime);
						AmqpDriver.this.connectResource();
						tries++;
					} catch (InterruptedException e) {
						if (AmqpDriver.super.isDestroyed()) {
							break;
						}
						ExceptionTracer.traceDeferred(e);
					}
				}
				if (!AmqpDriver.this.connected
						&& !AmqpDriver.super.isDestroyed()) {
					MosaicLogger.getLogger().error(
							"Could not reconnect to AMQP resource."); //$NON-NLS-1$
				}
			}
		}

	}

}
