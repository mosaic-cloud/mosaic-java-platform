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

package eu.mosaic_cloud.cloudlets.runtime;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.SupplementaryEnvironment;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;

public final class CloudletEnvironment {

    public static final CloudletEnvironment create(final IConfiguration configuration,
            final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass,
            final ClassLoader classLoader, final IConnectorsFactory connectors,
            final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions) {
        return (new CloudletEnvironment(configuration, cloudletCallbackClass, cloudletContextClass,
                classLoader, connectors, reactor, threading, exceptions,
                new HashMap<String, Object>()));
    }

    public static final CloudletEnvironment create(final IConfiguration configuration,
            final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass,
            final ClassLoader classLoader, final IConnectorsFactory connectors,
            final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions, final Map<String, Object> supplementary) {
        return (new CloudletEnvironment(configuration, cloudletCallbackClass, cloudletContextClass,
                classLoader, connectors, reactor, threading, exceptions, supplementary));
    }

    public final ClassLoader classLoader;

    public final Class<?> cloudletCallbackClass;

    public final Class<?> cloudletContextClass;

    public final IConfiguration configuration;

    public final IConnectorsFactory connectors;

    public final ExceptionTracer exceptions;

    public final CallbackReactor reactor;

    public final SupplementaryEnvironment supplementary;

    public final ThreadingContext threading;

    private CloudletEnvironment(final IConfiguration configuration,
            final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass,
            final ClassLoader classLoader, final IConnectorsFactory connectors,
            final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions, final Map<String, Object> supplementary) {
        super();
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(cloudletCallbackClass);
        Preconditions.checkNotNull(cloudletContextClass);
        Preconditions.checkNotNull(classLoader);
        Preconditions.checkNotNull(connectors);
        Preconditions.checkNotNull(reactor);
        Preconditions.checkNotNull(threading);
        Preconditions.checkNotNull(exceptions);
        Preconditions.checkNotNull(supplementary);
        this.configuration = configuration;
        this.cloudletCallbackClass = cloudletCallbackClass;
        this.cloudletContextClass = cloudletContextClass;
        this.classLoader = classLoader;
        this.connectors = connectors;
        this.reactor = reactor;
        this.threading = threading;
        this.exceptions = exceptions;
        this.supplementary = SupplementaryEnvironment.create(supplementary,
                new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(final Thread thread, final Throwable exception) {
                        exceptions.trace(ExceptionResolution.Ignored, exception);
                    }
                });
    }
}
