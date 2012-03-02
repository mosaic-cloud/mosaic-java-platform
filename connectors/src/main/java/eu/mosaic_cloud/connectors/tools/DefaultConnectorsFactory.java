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
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Resolver;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;


public class DefaultConnectorsFactory
		extends BaseConnectorsFactory
{
	protected DefaultConnectorsFactory (final IConnectorsFactory delegate)
	{
		super (delegate);
	}
	
	public static final DefaultConnectorsFactory create (final IConnectorsFactory delegate, final Channel channel, final Resolver resolver, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		final DefaultConnectorsFactory factory = new DefaultConnectorsFactory (delegate);
		DefaultConnectorsFactory.initialize (factory, channel, resolver, threading, exceptions);
		return (factory);
	}
	
	protected static final void initialize (final DefaultConnectorsFactory factory, final Channel channel, final Resolver resolver, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		Preconditions.checkNotNull (factory);
		factory.registerFactory (IKvStoreConnectorFactory.class, new IKvStoreConnectorFactory () {
			@Override
			public <Value> IKvStoreConnector<Value> create (final IConfiguration configuration, final Class<Value> valueClass, final DataEncoder<? super Value> valueEncoder)
			{
				return (GenericKvStoreConnector.create (configuration, valueEncoder, channel, resolver, threading, exceptions));
			}
		});
		factory.registerFactory (IMemcacheKvStoreConnectorFactory.class, new IMemcacheKvStoreConnectorFactory () {
			@Override
			public <Value> IMemcacheKvStoreConnector<Value> create (final IConfiguration configuration, final Class<Value> valueClass, final DataEncoder<? super Value> valueEncoder)
			{
				return (MemcacheKvStoreConnector.create (configuration, valueEncoder, channel, resolver, threading, exceptions));
			}
		});
		factory.registerFactory (IAmqpQueueRawConnectorFactory.class, new IAmqpQueueRawConnectorFactory () {
			@Override
			public IAmqpQueueRawConnector create (final IConfiguration configuration)
			{
				return (AmqpQueueRawConnector.create (configuration, channel, resolver, threading, exceptions));
			}
		});
		factory.registerFactory (IAmqpQueueConsumerConnectorFactory.class, new IAmqpQueueConsumerConnectorFactory () {
			@Override
			public <Message> IAmqpQueueConsumerConnector<Message> create (final IConfiguration configuration, final Class<Message> messageClass, final DataEncoder<? super Message> messageEncoder, final IAmqpQueueConsumerCallback<Message> callback)
			{
				return (AmqpQueueConsumerConnector.create (configuration, messageClass, messageEncoder, callback, channel, resolver, threading, exceptions));
			}
		});
		factory.registerFactory (IAmqpQueuePublisherConnectorFactory.class, new IAmqpQueuePublisherConnectorFactory () {
			@Override
			public <Message> IAmqpQueuePublisherConnector<Message> create (final IConfiguration configuration, final Class<Message> messageClass, final DataEncoder<? super Message> messageEncoder)
			{
				return (AmqpQueuePublisherConnector.create (configuration, messageClass, messageEncoder, channel, resolver, threading, exceptions));
			}
		});
	}
}
