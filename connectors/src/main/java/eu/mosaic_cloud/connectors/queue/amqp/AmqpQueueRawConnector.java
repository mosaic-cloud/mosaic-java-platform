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

import eu.mosaic_cloud.connectors.core.BaseConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Connector for queuing systems implementing the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpQueueRawConnector extends BaseConnector<AmqpQueueRawConnectorProxy> implements
        IAmqpQueueRawConnector {

    /**
     * Returns an AMQP connector. For AMQP it should always return a new
     * connector.
     * 
     * @param configuration
     *            the configuration parameters required by the connector. This
     *            should also include configuration settings for the
     *            corresponding driver.
     * @return the connector
     * @throws Throwable
     */
    public static AmqpQueueRawConnector create(final IConfiguration configuration,
            final ConnectorEnvironment environment) {
        final AmqpQueueRawConnectorProxy proxy = AmqpQueueRawConnectorProxy.create(configuration,
                environment);
        return new AmqpQueueRawConnector(proxy);
    }

    protected AmqpQueueRawConnector(final AmqpQueueRawConnectorProxy proxy) {
        super(proxy);
    }

    @Override
    public CallbackCompletion<Void> ack(final long delivery, final boolean multiple) {
        return this.proxy.ack(delivery, multiple);
    }

    @Override
    public CallbackCompletion<Void> bindQueue(final String exchange, final String queue,
            final String routingKey) {
        return this.proxy.bindQueue(exchange, queue, routingKey);
    }

    @Override
    public CallbackCompletion<Void> cancel(final String consumer) {
        return this.proxy.cancel(consumer);
    }

    @Override
    public CallbackCompletion<Void> consume(final String queue, final String consumer,
            final boolean exclusive, final boolean autoAck,
            final IAmqpQueueRawConsumerCallback consumerCallback) {
        return this.proxy.consume(queue, consumer, exclusive, autoAck, consumerCallback);
    }

    @Override
    public CallbackCompletion<Void> declareExchange(final String name, final AmqpExchangeType type,
            final boolean durable, final boolean autoDelete, final boolean passive) {
        return this.proxy.declareExchange(name, type, durable, autoDelete, passive);
    }

    @Override
    public CallbackCompletion<Void> declareQueue(final String queue, final boolean exclusive,
            final boolean durable, final boolean autoDelete, final boolean passive) {
        return this.proxy.declareQueue(queue, exclusive, durable, autoDelete, passive);
    }

    @Override
    public CallbackCompletion<Void> get(final String queue, final boolean autoAck) {
        return this.proxy.get(queue, autoAck);
    }

    @Override
    public CallbackCompletion<Void> publish(final AmqpOutboundMessage message) {
        return this.proxy.publish(message);
    }
}
