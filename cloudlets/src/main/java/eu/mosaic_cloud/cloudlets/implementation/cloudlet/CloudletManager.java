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

package eu.mosaic_cloud.cloudlets.implementation.cloudlet;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.tools.DefaultConnectorsFactory;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;

/**
 * Implements a container holding a list of cloudlet instances. All instances
 * have the same cloudlet type.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletManager {

    private final ChannelFactory channelFactory;
    private final ChannelResolver channelResolver;
    private final ConcurrentHashMap<Cloudlet<?>, Cloudlet<?>> cloudlets;
    private final IConfiguration configuration;
    private final TranscriptExceptionTracer exceptions;

    private final Monitor monitor = Monitor.create(this);
    private final CallbackReactor reactor;
    private final ThreadingContext threading;
    private ClassLoader classLoader;

    private CloudletManager(final IConfiguration configuration, final ClassLoader classLoader,
            final CallbackReactor reactor, final ThreadingContext threading,
            final ExceptionTracer exceptions, final ChannelFactory channelFactory,
            final ChannelResolver channelResolver) {
        super();
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(classLoader);
        Preconditions.checkNotNull(reactor);
        Preconditions.checkNotNull(threading);
        Preconditions.checkNotNull(exceptions);
        Preconditions.checkNotNull(channelFactory);
        Preconditions.checkNotNull(channelResolver);
        synchronized (this.monitor) {
            final Transcript transcript = Transcript.create(this);
            this.exceptions = TranscriptExceptionTracer.create(transcript, exceptions);
            this.configuration = configuration;
            this.reactor = reactor;
            this.threading = threading;
            this.channelFactory = channelFactory;
            this.channelResolver = channelResolver;
            this.cloudlets = new ConcurrentHashMap<Cloudlet<?>, Cloudlet<?>>();
            this.classLoader = classLoader;
        }
    }

    public static final CloudletManager create(final IConfiguration configuration,
            final ClassLoader classLoader, final CallbackReactor reactor,
            final ThreadingContext threading, final ExceptionTracer exceptions,
            final ChannelFactory channelFactory, final ChannelResolver channelResolver) {
        return (new CloudletManager(configuration, classLoader, reactor, threading, exceptions,
                channelFactory, channelResolver));
    }

    private final Cloudlet<?> createCloudletInstance() {
        final Class<?> cloudletCallbacksClass = this.resolveCloudletCallbacksClass();
        final Class<?> cloudletContextClass = this.resolveCloudletStateClass();
        final IConfiguration cloudletConfiguration = this.resolveCloudletConfiguration();

        final ConnectorEnvironment connectorEnvironment = ConnectorEnvironment.create(this.reactor,
                this.threading, this.exceptions, this.channelFactory, this.channelResolver);
        final IConnectorsFactory connectorFactory = DefaultConnectorsFactory.create(null,
                connectorEnvironment);
        final CloudletEnvironment environment = CloudletEnvironment.create(cloudletConfiguration,
                cloudletCallbacksClass, cloudletContextClass, this.classLoader, connectorFactory,
                this.reactor, this.threading, this.exceptions);
        final Cloudlet<?> cloudlet = Cloudlet.create(environment);
        return cloudlet;
    }

    public final boolean createInstance() {
        synchronized (this.monitor) {
            final Cloudlet<?> cloudlet = this.createCloudletInstance();
            cloudlet.initialize();
            this.cloudlets.put(cloudlet, cloudlet);
            return true;
        }
    }

    public final void destroy() {
        synchronized (this.monitor) {
            while (!this.cloudlets.isEmpty()) {
                this.destroyInstance();
            }
        }
    }

    public final boolean destroyInstance() {
        synchronized (this.monitor) {
            final Iterator<Cloudlet<?>> cloudletIterator = this.cloudlets.keySet().iterator();
            if (!cloudletIterator.hasNext()) {
                return (false);
            }
            final Cloudlet<?> cloudlet = cloudletIterator.next();
            cloudlet.destroy();
            this.cloudlets.remove(cloudlet);
            return true;
        }
    }

    private final Class<?> resolveCloudletCallbacksClass() {
        final String className = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("CloudletComponent.8"), String.class, null); //$NON-NLS-1$
        Preconditions.checkNotNull(className, "unknown cloudlet callbacks class");
        final Class<?> clasz;
        try {
            clasz = this.classLoader.loadClass(className);
        } catch (final ReflectiveOperationException exception) {
            this.exceptions.traceHandledException(exception);
            throw (new IllegalArgumentException(
                    "error encountered while loading cloudlet callbacks class", exception));
        }
        Preconditions.checkArgument(ICloudletCallback.class.isAssignableFrom(clasz),
                "invalid cloudlet callbacks class (must implement `ICloudletCallback`)");
        return clasz;
    }

    private final IConfiguration resolveCloudletConfiguration() {
        final String configurationDescriptor = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("CloudletComponent.10"), String.class, null); //$NON-NLS-1$
        Preconditions.checkNotNull(configurationDescriptor,
                "unknown cloudlet configuration descriptor");
        final IConfiguration configuration;
        try {
            configuration = PropertyTypeConfiguration.create(this.classLoader,
                    configurationDescriptor);
        } catch (final Throwable exception) {
            this.exceptions.traceHandledException(exception);
            throw (new IllegalArgumentException(
                    "error encountered while loading cloudlet configuration", exception));
        }
        return configuration;
    }

    private final Class<?> resolveCloudletStateClass() {
        final String className = ConfigUtils.resolveParameter(this.configuration,
                ConfigProperties.getString("CloudletComponent.9"), String.class, null); //$NON-NLS-1$
        Preconditions.checkNotNull(className, "unknown cloudlet context class");
        final Class<?> clasz;
        try {
            clasz = this.classLoader.loadClass(className);
        } catch (final ReflectiveOperationException exception) {
            this.exceptions.traceHandledException(exception);
            throw (new IllegalArgumentException(
                    "error encountered while loading cloudlet context class", exception));
        }
        return clasz;
    }
}
