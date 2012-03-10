/*
 * #%L
 * mosaic-drivers
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.drivers.queue.amqp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import eu.mosaic_cloud.drivers.AbstractResourceDriver;
import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.GenericResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Driver class for the AMQP-based management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpDriver extends AbstractResourceDriver { // NOPMD by georgiana

    // on 10/12/11 4:24
    // PM
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
            this.maxReconnectionTries = ConfigUtils.resolveParameter(AmqpDriver.this.configuration,
                    ConfigProperties.getString("AmqpDriver.6"), Integer.class, //$NON-NLS-1$
                    ConnectionShutdownListener.DEFAULT_MAX_RECONNECTION_TRIES);
            this.minReconnectionTime = ConfigUtils.resolveParameter(AmqpDriver.this.configuration,
                    ConfigProperties.getString("AmqpDriver.7"), Long.class, //$NON-NLS-1$
                    ConnectionShutdownListener.DEFAULT_MIN_RECONNECTION_TIME);
        }

        @Override
        public void shutdownCompleted(ShutdownSignalException arg0) {
            synchronized (AmqpDriver.this) {
                if (AmqpDriver.super.isDestroyed()) {
                    return;
                }
                AmqpDriver.this.logger
                        .trace("AMQP server closed connection with driver. Trying to reconnect..."); //$NON-NLS-1$
                AmqpDriver.this.connected = false;
                int tries = 0;
                while (!AmqpDriver.this.connected && (tries < this.maxReconnectionTries)) {
                    try {
                        AmqpDriver.this.wait(this.minReconnectionTime);
                        AmqpDriver.this.connectResource();
                        tries++; // NOPMD by georgiana on 10/12/11 4:23 PM
                    } catch (final InterruptedException e) {
                        ExceptionTracer.traceIgnored(e);
                        if (AmqpDriver.super.isDestroyed()) {
                            break;
                        }
                        ExceptionTracer.traceDeferred(e);
                    }
                }
                if (!AmqpDriver.this.connected && !AmqpDriver.super.isDestroyed()) {
                    AmqpDriver.this.logger.error("Could not reconnect to AMQP resource."); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Message consumer class which will receive notifications and messages from
     * a queue by subscription.
     * <p>
     * Note: all methods of this class are invoked inside the Connection's
     * thread. This means they a) should be non-blocking and generally do little
     * work, b) must not call ChannelController or Connection methods, or a
     * deadlock will ensue.
     * 
     * @author Georgiana Macariu
     * 
     */
    final class ConsumerCallback implements Consumer { // NOPMD by georgiana on

        // 10/12/11 4:24 PM
        ConsumerCallback() {
            super();
        }

        @Override
        public void handleCancel(final String consumer) throws IOException {
            AmqpDriver.this.logger
                    .trace("AmqpDriver - Received CANCEL callback for consumer " + consumer //$NON-NLS-1$
                            + "."); //$NON-NLS-1$
            final IAmqpConsumer cancelCallback = AmqpDriver.this.consumers.remove(consumer);
            if (cancelCallback != null) {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        cancelCallback.handleCancel(consumer);
                    }
                };
                AmqpDriver.this.executor.execute(task);
            }
        }

        @Override
        public void handleCancelOk(final String consumer) {
            AmqpDriver.this.logger
                    .trace("AmqpDriver - Received CANCEL Ok callback for consumer " + consumer //$NON-NLS-1$
                            + "."); //$NON-NLS-1$
            final IAmqpConsumer cancelCallback = AmqpDriver.this.consumers.remove(consumer);
            AmqpDriver.this.channels.remove(consumer);
            if (cancelCallback != null) {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        cancelCallback.handleCancelOk(consumer);
                    }
                };
                AmqpDriver.this.executor.execute(task);
            }
        }

        @Override
        public void handleConsumeOk(final String consumer) {
            AmqpDriver.this.logger
                    .trace("AmqpDriver - Received CONSUME Ok callback for consumer " + consumer //$NON-NLS-1$
                            + "."); //$NON-NLS-1$
            final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers.get(consumer);
            if (consumeCallback == null) {
                AmqpDriver.this.logger
                        .error("AmqpDriver - no callback to handle CONSUME Ok message"); //$NON-NLS-1$
            } else {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        consumeCallback.handleConsumeOk(consumer);
                    }
                };
                AmqpDriver.this.executor.execute(task);
            }
        }

        @Override
        public void handleDelivery(final String consumer, final Envelope envelope,
                final AMQP.BasicProperties properties, final byte[] data) {
            final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers.get(consumer);
            if (consumeCallback != null) {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        final AmqpInboundMessage message = new AmqpInboundMessage(consumer,
                                envelope.getDeliveryTag(), envelope.getExchange(),
                                envelope.getRoutingKey(), data,
                                ((properties.getDeliveryMode() != null) && (properties
                                        .getDeliveryMode() == 2)) ? true : false,
                                properties.getReplyTo(), null, properties.getContentType(),
                                properties.getCorrelationId(), null);
                        consumeCallback.handleDelivery(message);
                    }
                };
                AmqpDriver.this.executor.execute(task);
            }
        }

        @Override
        public void handleRecoverOk(String consumerTag) {
            // NOTE: nothing to do here
        }

        @Override
        public void handleShutdownSignal(final String consumer, final ShutdownSignalException signal) {
            AmqpDriver.this.logger.trace("AmqpDriver - Received SHUTDOWN callback for consumer " //$NON-NLS-1$
                    + consumer + "."); //$NON-NLS-1$
            final IAmqpConsumer consumeCallback = AmqpDriver.this.consumers.remove(consumer);
            AmqpDriver.this.channels.remove(consumer);
            if (consumeCallback != null) {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        consumeCallback.handleShutdown(consumer, signal.getMessage());
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
        public void handleReturn(int replyCode, String replyMessage, String exchange,
                String routingKey, BasicProperties properties, byte[] data) throws IOException {
            final AmqpInboundMessage message = new AmqpInboundMessage(null, -1, exchange,
                    routingKey, data, properties.getDeliveryMode() == 2 ? true : false,
                    properties.getReplyTo(), properties.getContentEncoding(),
                    properties.getContentType(), properties.getCorrelationId(),
                    properties.getMessageId());
            AmqpDriver.this.logger
                    .trace("AmqpDriver - Received RETURN callback for " + message.getDelivery()); //$NON-NLS-1$
            // FIXME
        }
    }

    private boolean connected;

    private final IConfiguration configuration;
    private final AmqpOperationFactory opFactory;
    private Connection connection;
    private ConcurrentHashMap<String, Channel> channels;
    private final ReturnCallback returnCallback;
    private final ShutdownListener shutdownListener;
    protected final ConcurrentHashMap<String, IAmqpConsumer> consumers;
    private final ExecutorService executor;

    /**
     * Creates a new driver.
     * 
     * @param configuration
     *            configuration data required for starting the driver
     * @param noThreads
     *            number of threads to be used for serving requests
     */
    private AmqpDriver(IConfiguration configuration, ThreadingContext threading, int noThreads) {
        super(threading, noThreads);
        this.configuration = configuration;
        this.connected = false;
        this.opFactory = new AmqpOperationFactory(this);
        this.returnCallback = new ReturnCallback();
        this.shutdownListener = new ConnectionShutdownListener();
        this.consumers = new ConcurrentHashMap<String, IAmqpConsumer>();
        this.executor = threading.createFixedThreadPool(
                ThreadConfiguration.create(this, "operations", true), 1);
    }

    /**
     * Returns an AMQP driver.
     * 
     * @param configuration
     *            configuration data required for starting the driver
     * @return an AMQP driver
     */
    public static AmqpDriver create(IConfiguration configuration, ThreadingContext threading) { // NOPMD
                                                                                                // by
                                                                                                // georgiana
                                                                                                // on
                                                                                                // 10/12/11
                                                                                                // 4:19
                                                                                                // PM
        final int noThreads = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("AmqpDriver.0"), Integer.class, 1); //$NON-NLS-1$
        AmqpDriver driver = new AmqpDriver(configuration, threading, noThreads);
        // NOTE: open connection - moved to the stub
        driver.connectResource();
        if (!driver.connected) {
            driver = null; // NOPMD by georgiana on 10/12/11 3:38 PM
        }
        return driver;
    }

    /**
     * Acknowledge one or several received messages.
     * 
     * @param clientId
     *            client identifier
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
    public IResult<Boolean> basicAck(String clientId, long delivery, boolean multiple,
            IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.ACK, delivery, multiple, clientId);
        return startOperation(operation, complHandler);
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
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.CANCEL, consumer);
        return startOperation(operation, complHandler);
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
    public IResult<String> basicConsume(String queue, String consumer, boolean exclusive,
            boolean autoAck, IAmqpConsumer consumeCallback,
            IOperationCompletionHandler<String> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<String> operation = (GenericOperation<String>) this.opFactory
                .getOperation(AmqpOperations.CONSUME, queue, consumer, exclusive, autoAck,
                        consumeCallback);
        return startOperation(operation, complHandler);
    }

    /**
     * Retrieve a message from a queue.
     * 
     * @param clientId
     *            client identifier
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
    public IResult<Boolean> basicGet(String clientId, String queue, boolean autoAck,
            IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.GET, queue, autoAck, clientId);
        return startOperation(operation, complHandler);
    }

    /**
     * Publishes a message.
     * 
     * @param clientId
     *            client identifier
     * @param message
     *            the message, message properties and destination data
     * @param complHandler
     *            handlers to be called when the operation finishes
     * @return <code>true</code> if message was published successfully
     */
    public IResult<Boolean> basicPublish(String clientId, AmqpOutboundMessage message,
            IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.PUBLISH, message, clientId);
        return startOperation(operation, complHandler);
    }

    /**
     * Bind a queue to an exchange, with no extra arguments.
     * 
     * @param clientId
     *            client identifier
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
    public IResult<Boolean> bindQueue(String clientId, String exchange, String queue,
            String routingKey, IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.BIND_QUEUE, exchange, queue, routingKey, clientId);
        return startOperation(operation, complHandler);
    }

    private synchronized void connectResource() {
        final String amqpServerHost = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("AmqpDriver.1"), String.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_HOST);
        final int amqpServerPort = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("AmqpDriver.2"), //$NON-NLS-1$
                Integer.class, ConnectionFactory.DEFAULT_AMQP_PORT);
        final String amqpServerUser = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("AmqpDriver.3"), String.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_USER);
        final String amqpVirtualHost = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("AmqpDriver.5"), //$NON-NLS-1$
                String.class, ConnectionFactory.DEFAULT_VHOST);
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpServerHost);
        factory.setPort(amqpServerPort);
        if (!"".equals(amqpVirtualHost)) {
            factory.setVirtualHost(amqpVirtualHost);
        }
        if (!amqpServerUser.isEmpty()) {
            final String amqpServerPasswd = ConfigUtils.resolveParameter(this.configuration,
                    ConfigProperties.getString("AmqpDriver.4"), String.class, //$NON-NLS-1$
                    ConnectionFactory.DEFAULT_PASS);
            factory.setUsername(amqpServerUser);
            factory.setPassword(amqpServerPasswd);
        }
        try {
            this.connection = factory.newConnection();
            this.connection.addShutdownListener(this.shutdownListener);
            this.channels = new ConcurrentHashMap<String, Channel>();
            this.connected = true;
            this.logger.debug("AMQP driver connected to " + amqpServerHost + ":" + amqpServerPort);
        } catch (final IOException e) {
            ExceptionTracer.traceIgnored(e);
            this.connection = null; // NOPMD by georgiana on 10/12/11 3:38
                                    // PM
        }
    }

    /**
     * Declares an exchange and creates a channel for it.
     * 
     * @param clientId
     *            client identifier
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
    public IResult<Boolean> declareExchange(String clientId, String name, AmqpExchangeType type,
            boolean durable, boolean autoDelete, boolean passive,
            IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.DECLARE_EXCHANGE, name, type, durable, autoDelete,
                        passive, clientId);
        return startOperation(operation, complHandler);
    }

    /**
     * Declare a queue.
     * 
     * @param clientId
     *            client identifier
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
    public IResult<Boolean> declareQueue(String clientId, String queue, boolean exclusive,
            boolean durable, boolean autoDelete, boolean passive,
            IOperationCompletionHandler<Boolean> complHandler) {
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) this.opFactory
                .getOperation(AmqpOperations.DECLARE_QUEUE, queue, exclusive, durable, autoDelete,
                        passive, clientId);
        return startOperation(operation, complHandler);
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        // NOTE: close any existing connection
        if (this.connected) {
            try {
                for (final Map.Entry<String, Channel> channel : AmqpDriver.this.channels.entrySet()) {
                    // FIXME
                    try {
                        channel.getValue().close();
                    } catch (final AlreadyClosedException e) {
                        ExceptionTracer.traceHandled(e);
                    }
                }
                this.connection.close();
                this.connected = false;
            } catch (final IOException e) {
                ExceptionTracer.traceIgnored(e);
                this.logger.error("AMQP cannot close connection with server."); //$NON-NLS-1$
            }
        }
        this.executor.shutdown();
        this.logger.trace("AmqpDriver destroyed."); //$NON-NLS-1$
    }

    protected Channel getChannel(String clientId) {
        Channel channel = this.channels.get(clientId);
        if (channel == null) {
            channel = this.openChannel(clientId);
        }
        return channel;
    }

    private synchronized Channel openChannel(String clientId) {
        Channel channel = null; // NOPMD by georgiana on 10/12/11 4:21 PM
        try {
            if (this.connected) {
                channel = this.connection.createChannel();
                channel.setDefaultConsumer(null);
                channel.addReturnListener(this.returnCallback);
                channel.basicQos(1);
                this.channels.put(clientId, channel);
            }
        } catch (final IOException e) {
            ExceptionTracer.traceIgnored(e);
        }
        return channel;
    }

    @SuppressWarnings({
            "unchecked", "rawtypes" })
    private <T extends Object> IResult<T> startOperation(GenericOperation<T> operation,
            IOperationCompletionHandler complHandler) {
        final IResult<T> iResult = new GenericResult<T>(operation);
        operation.setHandler(complHandler);
        super.addPendingOperation(iResult);
        super.submitOperation(operation.getOperation());
        return iResult;
    }
}
