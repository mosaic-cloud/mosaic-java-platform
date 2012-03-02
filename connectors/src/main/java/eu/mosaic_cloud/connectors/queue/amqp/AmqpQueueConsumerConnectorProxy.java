/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.queue.amqp;

import java.util.UUID;

import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Resolver;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * This class provides access for cloudlets to an AMQP-based queueing system as
 * a message consumer.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Message>
 *            the type of the messages consumed by the cloudlet
 */
public final class AmqpQueueConsumerConnectorProxy<Message> extends
        AmqpQueueConnectorProxy<Message> implements IAmqpQueueConsumerConnector<Message> {

    protected class AmqpConsumerCallback extends Object implements IAmqpQueueRawConsumerCallback {

        protected final IAmqpQueueConsumerCallback<Message> delegate;

        protected AmqpConsumerCallback(final IAmqpQueueConsumerCallback<Message> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public CallbackCompletion<Void> handleCancelOk(final String consumerTag) {
            return (CallbackCompletion.createOutcome());
        }

        @Override
        public CallbackCompletion<Void> handleConsumeOk(final String consumerTag) {
            return (CallbackCompletion.createOutcome());
        }

        @Override
        public CallbackCompletion<Void> handleDelivery(final AmqpInboundMessage inbound) {
            final DeliveryToken delivery = new DeliveryToken(inbound.getDelivery());
            final Message message;
            try {
                message = (Message) AmqpQueueConsumerConnectorProxy.this.messageEncoder.decode(inbound
                        .getData());
            } catch (final EncodingException exception) {
            	// FIXME
                FallbackExceptionTracer.defaultInstance.traceDeferredException(exception);
                return (CallbackCompletion.createFailure(exception));
            }
            return this.delegate.consume(delivery, message);
        }

        @Override
        public CallbackCompletion<Void> handleShutdownSignal(final String consumerTag,
                final String message) {
            return CallbackCompletion.createOutcome();
        }
    }

    protected static class DeliveryToken extends Object implements IAmqpQueueDeliveryToken {

        public final long token;

        DeliveryToken(final long token) {
            super();
            this.token = token;
        }
    }

    protected final String bindingRoutingKey;

    protected final AmqpConsumerCallback callback;

    protected final boolean consumerAutoAck;

    protected final boolean definePassive;

    protected final String exchange;

    protected final boolean exchangeAutoDelete;

    protected final boolean exchangeDurable;

    protected final AmqpExchangeType exchangeType;

    protected final String identity;

    protected final String queue;

    protected final boolean queueAutoDelete;

    protected final boolean queueDurable;

    protected final boolean queueExclusive;

    /**
     * Creates a new AMQP queue consumer.
     * 
     * @param configuration
     *            configuration data required by the accessor:
     *            <ul>
     *            <li>amqp.consumer.queue - name of the queue from which to
     *            consume messages</li>
     *            <li>amqp.consumer.consumer_id - an if of this consumer</li>
     *            <li>amqp.consumer.auto_ack - true if the server should
     *            consider messages acknowledged once delivered; false if the
     *            server should expect explicit acknowledgements</li>
     *            <li>amqp.consumer.exclusive - true if this is an exclusive
     *            consumer</li>
     *            </ul>
     * @param cloudlet
     *            the cloudlet controller of the cloudlet using the accessor
     * @param messageClass
     *            the type of the consumed messages
     * @param messageEncoder
     *            encoder used for serializing data
     */
    protected AmqpQueueConsumerConnectorProxy(final AmqpQueueRawConnectorProxy rawProxy,
            final IConfiguration configuration, final Class<Message> messageClass,
            final DataEncoder<? super Message> messageEncoder,
            final IAmqpQueueConsumerCallback<Message> callback) {
        super(rawProxy, configuration, messageClass, messageEncoder);
        this.identity = UUID.randomUUID().toString();
        this.exchange = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("AmqpQueueConnector.0"), String.class, this.identity); //$NON-NLS-1$ 
        this.exchangeType = ConfigUtils
                .resolveParameter(
                        configuration,
                        ConfigProperties.getString("AmqpQueueConnector.5"), AmqpExchangeType.class, AmqpExchangeType.DIRECT);//$NON-NLS-1$
        this.exchangeDurable = ConfigUtils
                .resolveParameter(
                        configuration,
                        ConfigProperties.getString("AmqpQueueConnector.9"), Boolean.class, Boolean.FALSE).booleanValue(); //$NON-NLS-1$ 
        this.exchangeAutoDelete = ConfigUtils
                .resolveParameter(
                        configuration,
                        ConfigProperties.getString("AmqpQueueConnector.7"), Boolean.class, Boolean.TRUE).booleanValue(); //$NON-NLS-1$
        this.queue = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("AmqpQueueConnector.2"), String.class, this.identity); //$NON-NLS-1$ 
        this.queueExclusive = ConfigUtils
                .resolveParameter(
                        configuration,
                        ConfigProperties.getString("AmqpQueueConnector.6"), Boolean.class, Boolean.FALSE).booleanValue(); //$NON-NLS-1$ 
        this.queueAutoDelete = this.exchangeAutoDelete;
        this.queueDurable = this.exchangeDurable;
        this.bindingRoutingKey = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("AmqpQueueConnector.1"), String.class, this.identity); //$NON-NLS-1$ 
        this.consumerAutoAck = ConfigUtils
                .resolveParameter(
                        this.config,
                        ConfigProperties.getString("AmqpQueueConnector.10"), Boolean.class, Boolean.FALSE).booleanValue(); //$NON-NLS-1$ 
        this.definePassive = ConfigUtils
                .resolveParameter(
                        configuration,
                        ConfigProperties.getString("AmqpQueueConnector.8"), Boolean.class, Boolean.FALSE).booleanValue(); //$NON-NLS-1$ 
        this.callback = new AmqpConsumerCallback(callback);
    }

    public static <Message> AmqpQueueConsumerConnectorProxy<Message> create(
            final IConfiguration configuration, final Channel channel, final Resolver resolver,
            final ThreadingContext threading, final ExceptionTracer exceptions,
            final Class<Message> messageClass, final DataEncoder<? super Message> messageEncoder,
            final IAmqpQueueConsumerCallback<Message> callback) {
        final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create(
                configuration, channel, resolver, threading, exceptions);
        final IConfiguration subConfiguration = configuration
                .spliceConfiguration(ConfigurationIdentifier.resolveRelative("publisher"));
        final AmqpQueueConsumerConnectorProxy<Message> proxy = new AmqpQueueConsumerConnectorProxy<Message>(
                rawProxy, subConfiguration, messageClass, messageEncoder, callback);
        channel.register(AmqpSession.CONNECTOR);
        return proxy;
    }

    @Override
    public CallbackCompletion<Void> acknowledge(final IAmqpQueueDeliveryToken delivery) {
        return this.raw.ack(((DeliveryToken) delivery).token, false);
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        // FIXME
        this.raw.cancel(this.identity);
        return this.raw.destroy();
    }

    @Override
    public CallbackCompletion<Void> initialize() {
        // FIXME
        this.raw.declareExchange(this.exchange, this.exchangeType, this.exchangeDurable,
                this.exchangeAutoDelete, this.definePassive);
        this.raw.declareQueue(this.queue, this.queueExclusive, this.queueDurable,
                this.queueAutoDelete, this.definePassive);
        this.raw.bindQueue(this.exchange, this.queue, this.bindingRoutingKey);
        return this.raw.consume(this.queue, this.identity, this.queueExclusive,
                this.consumerAutoAck, this.callback);
    }
}
