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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.cloudlets.core.CloudletException;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.tools.ChannelData;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture.Trigger;

import com.google.common.base.Preconditions;

/**
 * This callback class enables the container to communicate with other platform
 * components. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletComponentCallbacks implements ComponentCallbacks, CallbackHandler {

    /**
     * Supported resource types.
     * 
     * @author Georgiana Macariu
     * 
     */
    public static enum ResourceType {
        // NOTE: MEMCACHED is not yet supported, but will be in the near future
        AMQP("queue"),
        KEY_VALUE("kvstore"),
        MEMCACHED("kvstore");

        private final String configPrefix;

        ResourceType(String configPrefix) {
            this.configPrefix = configPrefix;
        }

        public String getConfigPrefix() {
            return this.configPrefix;
        }
    }

    static enum Status {
        Created, Terminated, Unregistered, Ready;
    }

    public static CloudletComponentCallbacks callbacks = null;
    private static MosaicLogger logger = MosaicLogger
            .createLogger(CloudletComponentCallbacks.class);
    private Status status;
    private ComponentController component;
    private final ComponentEnvironment componentEnvironment;
    private final IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> pendingReferences;
    private final ComponentIdentifier selfGroup;
    private final List<CloudletManager> cloudletRunners = new ArrayList<CloudletManager>();
    private final ExceptionTracer exceptions;

    /**
     * Creates a callback which is used by the mOSAIC platform to communicate
     * with the connectors.
     */
    public CloudletComponentCallbacks(ComponentEnvironment componentEnvironment) {
        super();
        this.componentEnvironment = componentEnvironment;
        this.exceptions = this.componentEnvironment.exceptions;
        this.pendingReferences = new IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>>();
        CloudletComponentCallbacks.callbacks = this;
        final IConfiguration configuration = PropertyTypeConfiguration.create(
                CloudletComponentCallbacks.class.getClassLoader(),
                "eu/mosaic_cloud/cloudlets/cloudlet-component.properties"); //$NON-NLS-1$
        this.selfGroup = ComponentIdentifier.resolve(ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("CloudletComponent.3"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
        this.status = Status.Created;
    }

    @Override
    public CallbackCompletion<Void> called(ComponentController component,
            ComponentCallRequest request) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        throw new UnsupportedOperationException();
    }

    @Override
    public CallbackCompletion<Void> callReturned(ComponentController component,
            ComponentCallReply reply) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState(this.status == Status.Ready);
        if (this.pendingReferences.containsKey(reply.reference)) {
            final Trigger<ComponentCallReply> trigger = this.pendingReferences
                    .remove(reply.reference);
            trigger.triggerSucceeded(reply);
        } else {
            throw (new IllegalStateException());
        }
        return null;
    }

    @Override
    public CallbackCompletion<Void> casted(ComponentController component,
            ComponentCastRequest request) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        throw (new UnsupportedOperationException());
    }

    @Override
    public CallbackCompletion<Void> failed(ComponentController component, Throwable exception) {
        CloudletComponentCallbacks.logger.trace("ComponentController container failed "
                + exception.getMessage());
        Preconditions.checkState(this.component == component);
        Preconditions.checkState(this.status != Status.Terminated);
        Preconditions.checkState(this.status != Status.Unregistered);
        // FIXME: also stop and destroy connector & cloudlets
        for (final CloudletManager container : this.cloudletRunners) {
            container.stop();
        }
        this.component = null;
        this.status = Status.Terminated;
        this.exceptions.trace(ExceptionResolution.Handled, exception);
        return null;
    }

    @Override
    public void failedCallbacks(Callbacks trigger, Throwable exception) {
    }

    /**
     * Sends a request to the platform in order to find a driver for a resource
     * of the specified type. Returns a future object which can be used for
     * waiting for the reply and retrieving the response.
     * 
     * @param type
     *            the type of the resource for which a driver is requested
     * @return a future object which can be used for waiting for the reply and
     *         retrieving the response
     */
    public ChannelData findDriver(ResourceType type) {
        CloudletComponentCallbacks.logger.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
        Preconditions.checkState(this.status == Status.Ready);
        final ComponentCallReference callReference = ComponentCallReference.create();
        final DeferredFuture<ComponentCallReply> replyFuture = DeferredFuture
                .create(ComponentCallReply.class);
        // FIXME
        ComponentIdentifier componentId = null;
        ComponentCallReply reply;
        ChannelData channel = null;
        this.pendingReferences.put(callReference, replyFuture.trigger);
        this.component.call(componentId, ComponentCallRequest.create(
                ConfigProperties.getString("CloudletComponent.7"), null, callReference)); //$NON-NLS-1$
        try {
            reply = replyFuture.get();
            if (reply.outputsOrError instanceof Map) {
                final Map<String, String> outcome = (Map<String, String>) reply.outputsOrError;
                channel = new ChannelData(outcome.get("channelIdentifier"),
                        outcome.get("channelEndpoint"));
                CloudletComponentCallbacks.logger.debug("Found driver on channel " + channel);
            }
        } catch (final InterruptedException e) {
            this.exceptions.trace(ExceptionResolution.Ignored, e);
        } catch (final ExecutionException e) {
            this.exceptions.trace(ExceptionResolution.Ignored, e);
        }
        return channel;
    }

    @Override
    public CallbackCompletion<Void> initialized(ComponentController component) {
        Preconditions.checkState(this.component == null);
        Preconditions.checkState(this.status == Status.Created);
        this.component = component;
        this.status = Status.Unregistered;
        final ComponentCallReference callReference = ComponentCallReference.create();
        this.component.register(this.selfGroup, callReference);
        final DeferredFuture<ComponentCallReply> result = DeferredFuture
                .create(ComponentCallReply.class);
        this.pendingReferences.put(callReference, result.trigger);
        CloudletComponentCallbacks.logger.trace("Container component callback initialized."); //$NON-NLS-1$
        return null;
    }

    @Override
    public void registeredCallbacks(Callbacks trigger, CallbackIsolate isolate) {
    }

    @Override
    public CallbackCompletion<Void> registerReturned(ComponentController component,
            ComponentCallReference reference, boolean ok) {
        Preconditions.checkState(this.component == component);
        final Trigger<ComponentCallReply> pendingReply = this.pendingReferences.remove(reference);
        if (pendingReply != null) {
            if (!ok) {
                final Exception e = new Exception("failed registering to group; terminating!"); //$NON-NLS-1$
                this.exceptions.trace(ExceptionResolution.Deferred, e);
                this.component.terminate();
                throw (new IllegalStateException(e));
            }
            this.status = Status.Ready;
            CloudletComponentCallbacks.logger
                    .info("Container component callback registered to group " + this.selfGroup); //$NON-NLS-1$
            final String properties = this.componentEnvironment.supplementary.get("descriptor", String.class, null);
            if (properties != null) {
                final ClassLoader loader = this.componentEnvironment.classLoader;
                final List<CloudletManager> containers = startCloudlet(loader,
                        properties);
                if (containers != null) {
                    this.cloudletRunners.addAll(containers);
                }
            } else {
                CloudletComponentCallbacks.logger.error("Missing config file");
            }
        } else {
            throw (new IllegalStateException());
        }
        return null;
    }

    private List<CloudletManager> startCloudlet(ClassLoader loader, String configurationFile) {
        final IConfiguration configuration = PropertyTypeConfiguration.create(loader,
                configurationFile);
        if (configuration == null) {
            CloudletComponentCallbacks.logger.error("Cloudlet configuration file "
                    + configurationFile + " is missing.");
            return null;
        }
        final int noInstances = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("CloudletComponent.11"), Integer.class, 1);
        final List<CloudletManager> containers = new ArrayList<CloudletManager>();
        for (int i = 0; i < noInstances; i++) {
            final CloudletManager container = new CloudletManager(
                    this.componentEnvironment.reactor, this.componentEnvironment.threading,
                    this.componentEnvironment.exceptions, loader, configuration);
            try {
                container.start();
                containers.add(container);
                CloudletComponentCallbacks.logger.trace("Starting cloudlet with config file "
                        + configurationFile);
            } catch (final CloudletException e) {
                this.exceptions.trace(ExceptionResolution.Ignored, e);
            }
        }
        return containers;
    }

    public void terminate() {
        Preconditions.checkState(this.component != null);
        this.component.terminate();
    }

    @Override
    public CallbackCompletion<Void> terminated(ComponentController component) {
        CloudletComponentCallbacks.logger.info("Container component callback terminating.");
        Preconditions.checkState(this.component == component);
        Preconditions.checkState(this.status != Status.Terminated);
        Preconditions.checkState(this.status != Status.Unregistered);
        // FIXME: also stop and destroy connector & cloudlets
        for (final CloudletManager container : this.cloudletRunners) {
            container.stop();
        }
        this.component = null;
        this.status = Status.Terminated;
        CloudletComponentCallbacks.logger.info("Container component callback terminated."); //$NON-NLS-1$
        return null;
    }

    @Override
    public void unregisteredCallbacks(Callbacks trigger) {
    }
}
