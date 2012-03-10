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

import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class AmqpQueuePublisherConnector<TMessage> extends
        AmqpQueueConnector<AmqpQueuePublisherConnectorProxy<TMessage>> implements
        IAmqpQueuePublisherConnector<TMessage> {

    protected AmqpQueuePublisherConnector(final AmqpQueuePublisherConnectorProxy<TMessage> proxy) {
        super(proxy);
    }

    public static <M> AmqpQueuePublisherConnector<M> create(final IConfiguration configuration,
            final ConnectorEnvironment environment, final Class<M> messageClass,
            final DataEncoder<M> messageEncoder) {
        final AmqpQueuePublisherConnectorProxy<M> proxy = AmqpQueuePublisherConnectorProxy.create(
                configuration, environment, messageClass, messageEncoder);
        return new AmqpQueuePublisherConnector<M>(proxy);
    }

    @Override
    public CallbackCompletion<Void> publish(final TMessage message) {
        return this.proxy.publish(message);
    }
}
