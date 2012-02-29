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

package eu.mosaic_cloud.drivers.interop.queue.amqp; // NOPMD by georgiana on 10/12/11 3:32 PM

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import eu.mosaic_cloud.drivers.IResourceDriver;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.drivers.interop.DriverConnectionData;
import eu.mosaic_cloud.drivers.interop.ResponseTransmitter;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpDriver;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpOperations;
import eu.mosaic_cloud.drivers.queue.amqp.IAmqpConsumer;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.Ack;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.BindQueueRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.CancelRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.ConsumeRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.DeclareExchangeRequest.ExchangeType;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.DeclareQueueRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.GetRequest;
import eu.mosaic_cloud.platform.interop.idl.amqp.AmqpPayloads.PublishRequest;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Stub for the driver for queuing systems implementing the AMQP protocol. This
 * is used for communicating with a AMQP driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpStub extends AbstractDriverStub { // NOPMD by georgiana on

    // 10/12/11 3:32 PM
    final class ConsumerHandler implements IAmqpConsumer {

        private final Session session;

        public ConsumerHandler(Session session) {
            super();
            this.session = session;
        }

        @Override
        public void handleCancel(String consumerTag) {
            final AmqpResponseTransmitter transmitter = AmqpStub.this
                    .getResponseTransmitter(AmqpResponseTransmitter.class);
            transmitter.sendCancel(this.session, consumerTag);
        }

        /*
         * (non-Javadoc)
         * 
         * @see <<<<<<<
         * HEAD:drivers/src/main/java/eu/mosaic_cloud/drivers/interop
         * /queue/amqp/AmqpStub.java
         * eu.mosaic_cloud.drivers.queue.IAmqpConsumer#
         * handleCancelOk(java.lang.String) =======
         * eu.mosaic_cloud.driver.queue.IAmqpConsumer#handleCancelOk(java.lang
         * .String) >>>>>>>
         * georgiana:drivers/src/main/java/eu/mosaic_cloud/driver
         * /interop/queue/amqp/AmqpStub.java
         */
        @Override
        public void handleCancelOk(String consumerTag) {
            final AmqpResponseTransmitter transmitter = AmqpStub.this
                    .getResponseTransmitter(AmqpResponseTransmitter.class);
            transmitter.sendCancelOk(this.session, consumerTag);
        }

        /*
         * (non-Javadoc)
         * 
         * @see <<<<<<<
         * HEAD:drivers/src/main/java/eu/mosaic_cloud/drivers/interop
         * /queue/amqp/AmqpStub.java
         * eu.mosaic_cloud.drivers.queue.IAmqpConsumer#
         * handleConsumeOk(java.lang.String) =======
         * eu.mosaic_cloud.driver.queue.IAmqpConsumer#handleConsumeOk(java.lang
         * .String) >>>>>>>
         * georgiana:drivers/src/main/java/eu/mosaic_cloud/driver
         * /interop/queue/amqp/AmqpStub.java
         */
        @Override
        public void handleConsumeOk(String consumerTag) {
            final AmqpResponseTransmitter transmitter = AmqpStub.this
                    .getResponseTransmitter(AmqpResponseTransmitter.class);
            transmitter.sendConsumeOk(this.session, consumerTag);
        }

        /*
         * (non-Javadoc)
         * 
         * @see <<<<<<<
         * HEAD:drivers/src/main/java/eu/mosaic_cloud/drivers/interop
         * /queue/amqp/AmqpStub.java
         * eu.mosaic_cloud.drivers.queue.IAmqpConsumer#
         * handleDelivery(mosaic.connector .queue.AmqpInboundMessage) =======
         * eu.mosaic_cloud.driver.queue.IAmqpConsumer#handleDelivery(mosaic.
         * connector .queue.AmqpInboundMessage) >>>>>>>
         * georgiana:drivers/src/main
         * /java/eu/mosaic_cloud/driver/interop/queue/amqp/AmqpStub.java
         */
        @Override
        public void handleDelivery(AmqpInboundMessage message) {
            final AmqpResponseTransmitter transmitter = AmqpStub.this
                    .getResponseTransmitter(AmqpResponseTransmitter.class);
            transmitter.sendDelivery(this.session, message);
        }

        /*
         * (non-Javadoc)
         * 
         * @see <<<<<<<
         * HEAD:drivers/src/main/java/eu/mosaic_cloud/drivers/interop
         * /queue/amqp/AmqpStub.java
         * eu.mosaic_cloud.drivers.queue.IAmqpConsumer#
         * handleShutdown(java.lang.String, java.lang.String) =======
         * eu.mosaic_cloud.driver.queue.IAmqpConsumer#handleShutdown(java.lang
         * .String, java.lang.String) >>>>>>>
         * georgiana:drivers/src/main/java/eu/
         * mosaic_cloud/driver/interop/queue/amqp/AmqpStub.java
         */
        @Override
        public void handleShutdown(String consumerTag, String errorMessage) {
            final AmqpResponseTransmitter transmitter = AmqpStub.this
                    .getResponseTransmitter(AmqpResponseTransmitter.class);
            transmitter.sendShutdownSignal(this.session, consumerTag, errorMessage);
        }
    }

    /**
     * Handler for processing responses of the requests submitted to the stub.
     * This will basically call the transmitter associated with the stub.
     * 
     * @author Georgiana Macariu
     * 
     */
    @SuppressWarnings("rawtypes")
    final class DriverOperationFinishedHandler implements IOperationCompletionHandler {

        private IResult<?> result;

        private AmqpOperations operation;

        private final CompletionToken complToken;

        private final CountDownLatch signal;

        private final AmqpDriver driver;

        private final AmqpResponseTransmitter transmitter;

        private final Session session;

        public DriverOperationFinishedHandler(CompletionToken complToken, Session session) {
            this.complToken = complToken;
            this.signal = new CountDownLatch(1);
            this.driver = AmqpStub.this.getDriver(AmqpDriver.class);
            this.transmitter = AmqpStub.this.getResponseTransmitter(AmqpResponseTransmitter.class);
            this.session = session;
        }

        @Override
        public void onFailure(Throwable error) {
            try {
                this.signal.await();
            } catch (final InterruptedException e) {
                ExceptionTracer.traceIgnored(e);
            }
            this.driver.removePendingOperation(this.result);
            // NOTE: result is error
            this.transmitter.sendResponse(this.session, this.complToken, this.operation,
                    error.getMessage(), true);
        }

        @Override
        public void onSuccess(Object response) {
            try {
                this.signal.await();
            } catch (final InterruptedException e) {
                ExceptionTracer.traceIgnored(e);
            }
            this.driver.removePendingOperation(this.result);
            this.transmitter.sendResponse(this.session, this.complToken, this.operation, response,
                    false);
        }

        public void setDetails(AmqpOperations operation, IResult<?> result) {
            this.operation = operation;
            this.result = result;
            this.signal.countDown();
        }
    }

    private static AmqpStub stub;

    /**
     * Creates a new stub for the AMQP driver.
     * 
     * @param config
     *            the configuration data for the stub and driver
     * @param transmitter
     *            the transmitter object which will send responses to requests
     *            submitted to this stub
     * @param driver
     *            the driver used for processing requests submitted to this stub
     * @param commChannel
     *            the channel for communicating with connectors
     */
    private AmqpStub(IConfiguration config, ResponseTransmitter transmitter,
            IResourceDriver driver, ZeroMqChannel commChannel) {
        super(config, transmitter, driver, commChannel);
    }

    /**
     * Returns a stub for the AMQP driver.
     * 
     * @param config
     *            the configuration data for the stub and driver
     * @param channel
     *            the channel for communicating with connectors
     * @return the AMQP driver stub
     */
    public static AmqpStub create(IConfiguration config, ZeroMqChannel channel,
            ThreadingContext threading) {
        final MosaicLogger sLogger = MosaicLogger.createLogger(AmqpStub.class);
        synchronized (AbstractDriverStub.MONITOR) {
            if (AmqpStub.stub == null) {
                final AmqpResponseTransmitter transmitter = new AmqpResponseTransmitter();
                final AmqpDriver driver = AmqpDriver.create(config, threading);
                AmqpStub.stub = new AmqpStub(config, transmitter, driver, channel);
                incDriverReference(AmqpStub.stub);
                channel.accept(AmqpSession.DRIVER, AmqpStub.stub);
                sLogger.trace("AmqpStub: created new stub."); //$NON-NLS-1$
            } else {
                sLogger.trace("AmqpStub: use existing stub."); //$NON-NLS-1$
                incDriverReference(AmqpStub.stub);
            }
        }
        return AmqpStub.stub;
    }

    /**
     * Reads resource connection data from the configuration data.
     * 
     * @param config
     *            the configuration data
     * @return resource connection data
     */
    protected static DriverConnectionData readConnectionData(IConfiguration config) {
        final String resourceHost = ConfigUtils.resolveParameter(config, "", String.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_HOST);
        final int resourcePort = ConfigUtils.resolveParameter(config, "", Integer.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_AMQP_PORT);
        final String amqpServerUser = ConfigUtils.resolveParameter(config, "", String.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_USER);
        final String amqpServerPasswd = ConfigUtils.resolveParameter(config, "", String.class, //$NON-NLS-1$
                ConnectionFactory.DEFAULT_PASS);
        DriverConnectionData cData;
        if (amqpServerUser.equals(ConnectionFactory.DEFAULT_USER)
                && amqpServerPasswd.equals(ConnectionFactory.DEFAULT_PASS)) {
            cData = new DriverConnectionData(resourceHost, resourcePort, "AMQP"); //$NON-NLS-1$
        } else {
            cData = new DriverConnectionData(resourceHost, resourcePort,
                    "AMQP", amqpServerUser, amqpServerPasswd); //$NON-NLS-1$
        }
        return cData;
    }

    @Override
    public synchronized void destroy() {
        synchronized (AbstractDriverStub.MONITOR) {
            decDriverReference(this);
        }
        super.destroy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void startOperation(Message message, Session session) // NOPMD by
                                                                    // georgiana
                                                                    // on
                                                                    // 10/12/11
                                                                    // 3:31 PM
            throws IOException, ClassNotFoundException {
        Preconditions.checkArgument(message.specification instanceof AmqpMessage);
        final AmqpMessage amqpMessage = (AmqpMessage) message.specification;
        CompletionToken token;
        IResult<Boolean> resultBool;
        IResult<String> resultString;
        String queue;
        String exchange;
        boolean durable;
        boolean autoDelete;
        boolean passive;
        boolean autoAck;
        boolean exclusive;
        String consumer;
        String routingKey;
        byte[] dataBytes;
        final AmqpDriver driver = super.getDriver(AmqpDriver.class); // NOPMD by
        // georgiana on
        // 10/12/11 3:24
        // PM
        switch (amqpMessage) {
        case ACCESS:
            this.logger.trace("Received initiation message"); //$NON-NLS-1$
            break;
        case ABORTED:
            this.logger.trace("Received termination message"); //$NON-NLS-1$
            break;
        case DECL_EXCHANGE_REQUEST:
            final AmqpPayloads.DeclareExchangeRequest declExchange = (DeclareExchangeRequest) message.payload;
            token = declExchange.getToken();
            exchange = declExchange.getExchange();
            final ExchangeType type = declExchange.getType();
            durable = declExchange.getDurable();
            autoDelete = declExchange.getAutoDelete();
            passive = declExchange.getPassive();
            this.logger.trace("AmqpStub - Received request for DECLARE EXCHANGE ");//$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler exchHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.declareExchange(token.getClientId(), exchange,
                    AmqpExchangeType.valueOf(type.toString().toUpperCase()), durable, autoDelete,
                    passive, exchHandler);
            exchHandler.setDetails(AmqpOperations.DECLARE_EXCHANGE, resultBool);
            break;
        case DECL_QUEUE_REQUEST:
            final AmqpPayloads.DeclareQueueRequest declQueue = (DeclareQueueRequest) message.payload;
            token = declQueue.getToken();
            queue = declQueue.getQueue();
            exclusive = declQueue.getExclusive();
            durable = declQueue.getDurable();
            autoDelete = declQueue.getAutoDelete();
            passive = declQueue.getPassive();
            this.logger.trace("AmqpStub - Received request for DECLARE QUEUE");//$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler queueHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.declareQueue(token.getClientId(), queue, exclusive, durable,
                    autoDelete, passive, queueHandler);
            queueHandler.setDetails(AmqpOperations.DECLARE_QUEUE, resultBool);
            break;
        case BIND_QUEUE_REQUEST:
            final AmqpPayloads.BindQueueRequest bindQueue = (BindQueueRequest) message.payload;
            token = bindQueue.getToken();
            exchange = bindQueue.getExchange();
            queue = bindQueue.getQueue();
            routingKey = bindQueue.getRoutingKey();
            this.logger.trace("AmqpStub - Received request for BIND QUEUE");//$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler bindHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.bindQueue(token.getClientId(), exchange, queue, routingKey,
                    bindHandler);
            bindHandler.setDetails(AmqpOperations.BIND_QUEUE, resultBool);
            break;
        case PUBLISH_REQUEST:
            final AmqpPayloads.PublishRequest publish = (PublishRequest) message.payload;
            token = publish.getToken();
            dataBytes = publish.getData().toByteArray();
            durable = publish.getDurable();
            exchange = publish.getExchange();
            final boolean immediate = publish.getImmediate();
            final boolean mandatory = publish.getMandatory();
            routingKey = publish.getRoutingKey();
            String correlationId = null;
            String replyTo = null;
            if (publish.hasCorrelationId()) {
                correlationId = publish.getCorrelationId();
            }
            if (publish.hasReplyTo()) {
                replyTo = publish.getReplyTo();
            }
            final AmqpOutboundMessage mssg = new AmqpOutboundMessage(exchange, routingKey,
                    dataBytes, mandatory, immediate, durable, replyTo, null,
                    publish.getContentType(), correlationId, null);
            this.logger.trace("AmqpStub - Received request for PUBLISH"); //$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler pubHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.basicPublish(token.getClientId(), mssg, pubHandler);
            pubHandler.setDetails(AmqpOperations.PUBLISH, resultBool);
            break;
        case CONSUME_REQUEST:
            final AmqpPayloads.ConsumeRequest cop = (ConsumeRequest) message.payload;
            token = cop.getToken();
            queue = cop.getQueue();
            consumer = cop.getConsumer();
            exclusive = cop.getExclusive();
            autoAck = cop.getAutoAck();
            this.logger.trace("AmqpStub - Received request for CONSUME"); //$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler consHandler = new DriverOperationFinishedHandler(
                    token, session);
            final IAmqpConsumer consumeCallback = new ConsumerHandler(session);
            resultString = driver.basicConsume(queue, consumer, exclusive, autoAck,
                    consumeCallback, consHandler);
            consHandler.setDetails(AmqpOperations.CONSUME, resultString);
            break;
        case GET_REQUEST:
            final AmqpPayloads.GetRequest gop = (GetRequest) message.payload;
            token = gop.getToken();
            queue = gop.getQueue();
            autoAck = gop.getAutoAck();
            this.logger.trace("AmqpStub - Received request for GET"); //$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler getHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.basicGet(token.getClientId(), queue, autoAck, getHandler);
            getHandler.setDetails(AmqpOperations.GET, resultBool);
            break;
        case CANCEL_REQUEST:
            final AmqpPayloads.CancelRequest clop = (CancelRequest) message.payload;
            token = clop.getToken();
            consumer = clop.getConsumer();
            this.logger.trace("AmqpStub - Received request for CANCEL"); //$NON-NLS-1$
            // NOTE: execute operation
            final DriverOperationFinishedHandler cancelHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.basicCancel(consumer, cancelHandler);
            cancelHandler.setDetails(AmqpOperations.CANCEL, resultBool);
            break;
        case ACK:
            final AmqpPayloads.Ack aop = (Ack) message.payload;
            token = aop.getToken();
            final long delivery = aop.getDelivery();
            final boolean multiple = aop.getMultiple();
            this.logger.trace("AmqpStub - Received  ACK "); //$NON-NLS-1$ 
            // NOTE: execute operation
            final DriverOperationFinishedHandler ackHandler = new DriverOperationFinishedHandler(
                    token, session);
            resultBool = driver.basicAck(token.getClientId(), delivery, multiple, ackHandler);
            ackHandler.setDetails(AmqpOperations.ACK, resultBool);
            break;
        default:
            final DriverOperationFinishedHandler errHandler = new DriverOperationFinishedHandler(
                    null, session);
            driver.handleUnsupportedOperationError(amqpMessage.toString(), errHandler);
            this.logger.error("Unknown amqp message: " + amqpMessage.toString()); //$NON-NLS-1$
            break;
        }
    }
}
