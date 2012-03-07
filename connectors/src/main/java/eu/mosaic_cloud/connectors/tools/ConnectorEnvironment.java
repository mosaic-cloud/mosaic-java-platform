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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.SupplementaryEnvironment;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;

public final class ConnectorEnvironment {

    public static final ConnectorEnvironment create(final CallbackReactor reactor,
            final ThreadingContext threading, final ExceptionTracer exceptions,
            final ChannelFactory channelFactory, final ChannelResolver channelResolver) {
        return (new ConnectorEnvironment(reactor, threading, exceptions, channelFactory,
                channelResolver, new HashMap<String, Object>()));
    }

    public static final ConnectorEnvironment create(final IConfiguration configuration,
            final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions, final ChannelFactory channelFactory,
            final ChannelResolver channelResolver, final Map<String, Object> supplementary) {
        return (new ConnectorEnvironment(reactor, threading, exceptions, channelFactory,
                channelResolver, supplementary));
    }

    public final ChannelFactory channelFactory;

    public final ChannelResolver channelResolver;

    public final ExceptionTracer exceptions;

    public final CallbackReactor reactor;

    public final SupplementaryEnvironment supplementary;

    public final ThreadingContext threading;

    private ConnectorEnvironment(final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions, final ChannelFactory channelFactory,
            final ChannelResolver channelResolver, final Map<String, Object> supplementary) {
        super();
        Preconditions.checkNotNull(reactor);
        Preconditions.checkNotNull(threading);
        Preconditions.checkNotNull(exceptions);
        Preconditions.checkNotNull(channelFactory);
        Preconditions.checkNotNull(channelResolver);
        Preconditions.checkNotNull(supplementary);
        this.reactor = reactor;
        this.threading = threading;
        this.exceptions = exceptions;
        this.channelFactory = channelFactory;
        this.channelResolver = channelResolver;
        this.supplementary = SupplementaryEnvironment.create(supplementary,
                new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(final Thread thread, final Throwable exception) {
                        exceptions.trace(ExceptionResolution.Ignored, exception);
                    }
                });
    }
}
