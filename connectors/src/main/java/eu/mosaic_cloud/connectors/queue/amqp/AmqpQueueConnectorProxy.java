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

import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;

public abstract class AmqpQueueConnectorProxy<TMessage> implements IAmqpQueueConnector {

    protected final ConnectorConfiguration configuration;
    protected final Class<TMessage> messageClass;
    protected final DataEncoder<TMessage> messageEncoder;
    protected final AmqpQueueRawConnectorProxy raw;

    protected AmqpQueueConnectorProxy(final AmqpQueueRawConnectorProxy raw,
            final ConnectorConfiguration configuration, final Class<TMessage> messageClass,
            final DataEncoder<TMessage> messageEncoder) {
        super();
        Preconditions.checkNotNull(raw);
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(messageClass);
        Preconditions.checkNotNull(messageEncoder);
        this.raw = raw;
        this.configuration = configuration;
        this.messageClass = messageClass;
        this.messageEncoder = messageEncoder;
    }
}
