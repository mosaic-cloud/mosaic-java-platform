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

import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;

public abstract class AmqpQueueConnectorProxy<Message> extends Object implements
        IAmqpQueueConnector {

    protected final IConfiguration config;

    protected final Class<Message> messageClass;

    protected final DataEncoder<? super Message> messageEncoder;

    protected final AmqpQueueRawConnectorProxy raw;

    protected AmqpQueueConnectorProxy(final AmqpQueueRawConnectorProxy raw,
            final IConfiguration config, final Class<Message> messageClass,
            final DataEncoder<? super Message> messageEncoder) {
        super();
        Preconditions.checkNotNull(raw);
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(messageClass);
        Preconditions.checkNotNull(messageEncoder);
        this.raw = raw;
        this.config = config;
        this.messageClass = messageClass;
        this.messageEncoder = messageEncoder;
    }
}
