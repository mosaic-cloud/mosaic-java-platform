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

package eu.mosaic_cloud.connectors.queue.amqp;


import eu.mosaic_cloud.connectors.core.BaseConnector;
import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


public class AmqpQueueConsumerConnector<Message>
		extends AmqpQueueConnector<AmqpQueueConsumerConnectorProxy<Message>>
		implements
			IAmqpQueueConsumerConnector<Message>
{
	protected AmqpQueueConsumerConnector (final AmqpQueueConsumerConnectorProxy<Message> proxy)
	{
		super (proxy);
	}
	
	@Override
	public CallbackCompletion<Void> acknowledge (final IAmqpQueueDeliveryToken delivery)
	{
		return this.proxy.acknowledge (delivery);
	}
	
	public static <Message> AmqpQueueConsumerConnector<Message> create (final IConfiguration configuration, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder, final IAmqpQueueConsumerCallback<Message> callback, final ThreadingContext threading)
	{
		final String driverIdentity = ConfigUtils.resolveParameter (configuration, ConfigProperties.getString ("GenericConnector.1"), String.class, "");
		final String driverEndpoint = ConfigUtils.resolveParameter (configuration, ConfigProperties.getString ("GenericConnector.0"), String.class, "");
		final Channel channel = BaseConnector.createChannel (driverEndpoint, threading);
		channel.register (AmqpSession.CONNECTOR);
		final AmqpQueueConsumerConnectorProxy<Message> proxy = AmqpQueueConsumerConnectorProxy.create (configuration, driverIdentity, channel, messageClass, messageEncoder, callback);
		return new AmqpQueueConsumerConnector<Message> (proxy);
	}
}
