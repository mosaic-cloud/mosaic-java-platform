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

package eu.mosaic_cloud.connectors.core;

import java.util.UUID;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

public abstract class BaseConnector<P extends BaseConnectorProxy> implements IConnector,
        CallbackProxy {
    protected final MosaicLogger logger;
    protected final P proxy;

    protected BaseConnector(final P proxy) {
        super();
        Preconditions.checkNotNull(proxy);
        this.proxy = proxy;
        this.logger = MosaicLogger.createLogger(this);
    }

    public static Channel createChannel(final String driverEndpoint,
            final ThreadingContext threading) {
        final String connectorIdentity = UUID.randomUUID().toString();
        final ZeroMqChannel channel = ZeroMqChannel.create(connectorIdentity, threading,
                AbortingExceptionTracer.defaultInstance);
        channel.connect(driverEndpoint);
        return channel;
    }

}
