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

package eu.mosaic_cloud.connectors.tools;

import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnectorFactory;
import eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueRawConnector;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueRawConnector;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueRawConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;

public class DefaultConnectorsFactory extends BaseConnectorsFactory {

    public static final DefaultConnectorsFactory create(
            final IConnectorsFactory delegate,
            final ConnectorEnvironment environment) {
        final DefaultConnectorsFactory factory = new DefaultConnectorsFactory(
                delegate);
        DefaultConnectorsFactory.initialize(factory, environment);
        return (factory);
    }

    protected static final void initialize(
            final DefaultConnectorsFactory factory,
            final ConnectorEnvironment environment) {
        Preconditions.checkNotNull(factory);
        Preconditions.checkNotNull(environment);
        factory.registerFactory(IKvStoreConnectorFactory.class,
                new IKvStoreConnectorFactory() {

                    @Override
                    public <TValue> IKvStoreConnector<TValue> create(
                            final IConfiguration configuration,
                            final Class<TValue> valueClass,
                            final DataEncoder<TValue> valueEncoder) {
                        return (GenericKvStoreConnector.create(
                                ConnectorConfiguration.create(configuration, environment),
                                valueEncoder));
                    }
                });
        factory.registerFactory(IMemcacheKvStoreConnectorFactory.class,
                new IMemcacheKvStoreConnectorFactory() {

                    @Override
                    public <TValue> IMemcacheKvStoreConnector<TValue> create(
                            final IConfiguration configuration,
                            final Class<TValue> valueClass,
                            final DataEncoder<TValue> valueEncoder) {
                        return (MemcacheKvStoreConnector.create(
                                ConnectorConfiguration.create(configuration, environment),
                                valueEncoder));
                    }
                });
        factory.registerFactory(IAmqpQueueRawConnectorFactory.class,
                new IAmqpQueueRawConnectorFactory() {

                    @Override
                    public IAmqpQueueRawConnector create(
                            final IConfiguration configuration) {
                        return (AmqpQueueRawConnector.create(
                                ConnectorConfiguration.create(configuration, environment)));
                    }
                });
        factory.registerFactory(IAmqpQueueConsumerConnectorFactory.class,
                new IAmqpQueueConsumerConnectorFactory() {

                    @Override
                    public <TMessage> IAmqpQueueConsumerConnector<TMessage> create(
                            final IConfiguration configuration,
                            final Class<TMessage> messageClass,
                            final DataEncoder<TMessage> messageEncoder,
                            final IAmqpQueueConsumerCallback<TMessage> callback) {
                        return (AmqpQueueConsumerConnector.create(
                                ConnectorConfiguration.create(configuration, environment),
                                messageClass, messageEncoder, callback));
                    }
                });
        factory.registerFactory(IAmqpQueuePublisherConnectorFactory.class,
                new IAmqpQueuePublisherConnectorFactory() {

                    @Override
                    public <TMessage> IAmqpQueuePublisherConnector<TMessage> create(
                            final IConfiguration configuration,
                            final Class<TMessage> messageClass,
                            final DataEncoder<TMessage> messageEncoder) {
                        return (AmqpQueuePublisherConnector.create(
                                ConnectorConfiguration.create(configuration, environment),
                                messageClass, messageEncoder));
                    }
                });
    }

    protected DefaultConnectorsFactory(final IConnectorsFactory delegate) {
        super(delegate);
    }
}
