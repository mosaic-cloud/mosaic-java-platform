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
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;

public final class AmqpQueueConsumerConnectorProxy<TMessage> extends
        AmqpQueueConnectorProxy<TMessage> implements IAmqpQueueConsumerConnector<TMessage> {

    protected class AmqpConsumerCallback implements IAmqpQueueRawConsumerCallback {

        protected final IAmqpQueueConsumerCallback<TMessage> delegate;

        protected AmqpConsumerCallback(final IAmqpQueueConsumerCallback<TMessage> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public CallbackCompletion<Void> handleCancelOk(final String consumerTag) {
            return CallbackCompletion.createOutcome();
        }

        @Override
        public CallbackCompletion<Void> handleConsumeOk(final String consumerTag) {
            return CallbackCompletion.createOutcome();
        }

        @Override
        public CallbackCompletion<Void> handleDelivery(final AmqpInboundMessage inbound) {
            final DeliveryToken delivery = new DeliveryToken(inbound.getDelivery());
            TMessage message = null;
            CallbackCompletion<Void> result = null;
            try {
                message = AmqpQueueConsumerConnectorProxy.this.messageEncoder.decode(inbound
                        .getData());
            } catch (final EncodingException exception) {
                FallbackExceptionTracer.defaultInstance.traceDeferredException(exception);
                result = CallbackCompletion.createFailure(exception);
            }
            if (result == null) {
                result = this.delegate.consume(delivery, message);
            }
            return result;
        }

        @Override
        public CallbackCompletion<Void> handleShutdownSignal(final String consumerTag,
                final String message) {
            return CallbackCompletion.createOutcome();
        }
    }

    protected static class DeliveryToken implements IAmqpQueueDeliveryToken {

        private final long token;

        DeliveryToken(final long token) {
            super();
            this.token = token;
        }

        public long getToken() {
            return this.token;
        }
    }

    private final AmqpConsumerCallback callback;
    private final String bindingRoutingKey;
    private final boolean consumerAutoAck;
    private final boolean definePassive;
    private final String exchange;
    private final boolean exchangeAutoDelete;
    private final boolean exchangeDurable;
    private final AmqpExchangeType exchangeType;
    private final String identity;
    private final String queue;
    private final boolean queueAutoDelete;
    private final boolean queueDurable;
    private final boolean queueExclusive;

    private AmqpQueueConsumerConnectorProxy(final AmqpQueueRawConnectorProxy rawProxy,
            final IConfiguration configuration, final Class<TMessage> messageClass,
            final DataEncoder<TMessage> messageEncoder,
            final IAmqpQueueConsumerCallback<TMessage> callback) {
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

    public static <M> AmqpQueueConsumerConnectorProxy<M> create(final IConfiguration configuration,
            final ConnectorEnvironment environment, final Class<M> messageClass,
            final DataEncoder<M> messageEncoder, final IAmqpQueueConsumerCallback<M> callback) {
        final AmqpQueueRawConnectorProxy rawProxy = AmqpQueueRawConnectorProxy.create(
                configuration, environment);
        final IConfiguration subConfiguration = configuration
                .spliceConfiguration(ConfigurationIdentifier.resolveRelative("publisher"));
        final AmqpQueueConsumerConnectorProxy<M> proxy = new AmqpQueueConsumerConnectorProxy<M>(
                rawProxy, subConfiguration, messageClass, messageEncoder, callback);
        return proxy;
    }

    @Override
    public CallbackCompletion<Void> acknowledge(final IAmqpQueueDeliveryToken delivery) {
        return this.raw.ack(((DeliveryToken) delivery).getToken(), false);
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        // FIXME: We should wait for `cancel` to succeed or fail, and then
        // continue.
        this.raw.cancel(this.identity);
        return this.raw.destroy();
    }

    @Override
    public CallbackCompletion<Void> initialize() {
        // FIXME: We should wait for each of these operation to either succeed
        // or fail before going further.
        this.raw.initialize();
        // FIXME: If any of these operations fail we should continue with
        // `destroy`.
        this.raw.declareExchange(this.exchange, this.exchangeType, this.exchangeDurable,
                this.exchangeAutoDelete, this.definePassive);
        this.raw.declareQueue(this.queue, this.queueExclusive, this.queueDurable,
                this.queueAutoDelete, this.definePassive);
        this.raw.bindQueue(this.exchange, this.queue, this.bindingRoutingKey);
        return this.raw.consume(this.queue, this.identity, this.queueExclusive,
                this.consumerAutoAck, this.callback);
    }
}
