/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.tools;

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.IMemcacheKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.IMemcacheKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.IMemcacheKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.MemcacheKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;

public class DefaultConnectorsFactory extends BaseConnectorsFactory implements
        IConnectorsFactory { // NOPMD 

    public static final DefaultConnectorsFactory create(
            final ICloudletController<?> cloudlet,
            final eu.mosaic_cloud.connectors.core.IConnectorsFactory delegate,
            final ThreadingContext threading, final ExceptionTracer exceptions) {
        final DefaultConnectorsFactory factory = new DefaultConnectorsFactory(
                delegate);
        DefaultConnectorsFactory.initialize(factory, cloudlet);
        return factory;
    }

    protected static final void initialize(
            final DefaultConnectorsFactory factory,
            final ICloudletController<?> cloudlet) {
        Preconditions.checkNotNull(factory);
        Preconditions.checkNotNull(cloudlet);
        factory.registerFactory(IKvStoreConnectorFactory.class,
                new IKvStoreConnectorFactory() {

                    @Override
                    public <TContext, TTValue, TExtra> IKvStoreConnector<TTValue, TExtra> create(
                            final IConfiguration configuration,
                            final Class<TTValue> valueClass,
                            final DataEncoder<TTValue> valueEncoder,
                            final IKvStoreConnectorCallback<TContext, TTValue, TExtra> callback,
                            final TContext callbackContext) {
                        final eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TTValue> backingConnector = (eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TTValue>) factory
                                .getConnectorFactory(
                                        eu.mosaic_cloud.connectors.kvstore.IKvStoreConnectorFactory.class)
                                .create(configuration, valueClass, valueEncoder);
                        return new GenericKvStoreConnector<TContext, TTValue, TExtra>(
                                cloudlet, backingConnector, configuration,
                                callback, callbackContext);
                    }
                });
        factory.registerFactory(IMemcacheKvStoreConnectorFactory.class,
                new IMemcacheKvStoreConnectorFactory() {

                    @Override
                    public <TContext, TValue, TExtra> IMemcacheKvStoreConnector<TValue, TExtra> create(
                            final IConfiguration configuration,
                            final Class<TValue> valueClass,
                            final DataEncoder<TValue> valueEncoder,
                            final IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra> callback,
                            final TContext callbackContext) {
                        final eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue> backingConnector = (eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue>) factory
                                .getConnectorFactory(
                                        eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnectorFactory.class)
                                .create(configuration, valueClass, valueEncoder);
                        return new MemcacheKvStoreConnector<TContext, TValue, TExtra>(
                                cloudlet, backingConnector, configuration,
                                callback, callbackContext);
                    }
                });
        factory.registerFactory(IAmqpQueueConsumerConnectorFactory.class,
                new IAmqpQueueConsumerConnectorFactory() {

                    @Override
                    public <TContext, Message, TExtra> IAmqpQueueConsumerConnector<Message, TExtra> create(
                            final IConfiguration configuration,
                            final Class<Message> messageClass,
                            final DataEncoder<Message> messageEncoder,
                            final IAmqpQueueConsumerConnectorCallback<TContext, Message, TExtra> callback,
                            final TContext callbackContext) {
                        final AmqpQueueConsumerConnector.Callback<Message> backingCallback = new AmqpQueueConsumerConnector.Callback<Message>();
                        final eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConsumerConnector<Message> backingConnector = (eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConsumerConnector<Message>) factory
                                .getConnectorFactory(
                                        eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory.class)
                                .create(configuration, messageClass,
                                        messageEncoder, backingCallback);
                        return new AmqpQueueConsumerConnector<TContext, Message, TExtra>(
                                cloudlet, backingConnector, configuration,
                                callback, callbackContext, backingCallback);
                    }
                });
        factory.registerFactory(IAmqpQueuePublisherConnectorFactory.class,
                new IAmqpQueuePublisherConnectorFactory() {

                    @Override
                    public <TContext, Message, TExtra> IAmqpQueuePublisherConnector<Message, TExtra> create(
                            final IConfiguration configuration,
                            final Class<Message> messageClass,
                            final DataEncoder<Message> messageEncoder,
                            final IAmqpQueuePublisherConnectorCallback<TContext, Message, TExtra> callback,
                            final TContext callbackContext) {
                        final eu.mosaic_cloud.connectors.queue.amqp.AmqpQueuePublisherConnector<Message> backingConnector = (eu.mosaic_cloud.connectors.queue.amqp.AmqpQueuePublisherConnector<Message>) factory
                                .getConnectorFactory(
                                        eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory.class)
                                .create(configuration, messageClass,
                                        messageEncoder);
                        return new AmqpQueuePublisherConnector<TContext, Message, TExtra>(
                                cloudlet, backingConnector, configuration,
                                callback, callbackContext);
                    }
                });
    }

    protected DefaultConnectorsFactory(
            final eu.mosaic_cloud.connectors.core.IConnectorsFactory delegate) {
        super(delegate);
    }
}
