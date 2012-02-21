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

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentContext;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture.Trigger;

/**
 * This callback class enables the connectors to find resource drivers and
 * connect to them. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ConnectorComponentCallbacks implements ComponentCallbacks, CallbackHandler {
    private final ComponentIdentifier amqpGroup;
    private ComponentController component;
    private final ComponentIdentifier kvGroup;
    private final ComponentIdentifier mcGroup;
    private final IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> pendingReferences;
    private final ComponentIdentifier selfGroup;
    private Status status;

    /**
     * Creates a callback which is used by the mOSAIC platform to communicate
     * with the connectors.
     */
    public ConnectorComponentCallbacks(final ComponentContext context) {
        super();
        this.pendingReferences = new IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>>();
        ConnectorComponentCallbacks.setComponentCallbacks(this);
        final IConfiguration configuration = PropertyTypeConfiguration.create(
                ConnectorComponentCallbacks.class.getClassLoader(), "resource-conn.properties"); //$NON-NLS-1$
        this.amqpGroup = ComponentIdentifier.resolve(ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("ResourceComponentCallbacks.0"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
        this.kvGroup = ComponentIdentifier.resolve(ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("ResourceComponentCallbacks.1"), //$NON-NLS-1$
                String.class, "")); //$NON-NLS-1$
        this.mcGroup = ComponentIdentifier.resolve(ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("ResourceComponentCallbacks.2"), String.class, //$NON-NLS-1$
                "")); //$NON-NLS-1$
        this.selfGroup = ComponentIdentifier.resolve(ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("ResourceComponentCallbacks.3"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
        this.status = Status.Created;
    }

    @Override
    public CallbackCompletion<Void> called(final ComponentController component,
            final ComponentCallRequest request) {
        ComponentCallReply reply = null; // NOPMD by georgiana on 2/21/12 2:49 PM
        boolean succeeded = false; // NOPMD by georgiana on 2/21/12 2:49 PM
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        if (this.status == Status.Ready) {
            if (request.operation
                    .equals(ConfigProperties.getString("ResourceComponentCallbacks.7"))) { //$NON-NLS-1$
                ConnectorComponentCallbacks.logger.debug("Testing AMQP connector"); //$NON-NLS-1$
                try {
                    succeeded = true;
                } catch (final Throwable e) {
                    ExceptionTracer.traceIgnored(e);
                }
                reply = ComponentCallReply.create(true, Boolean.valueOf(succeeded),
                        ByteBuffer.allocate(0), request.reference);
                component.callReturn(reply);
            } else if (request.operation.equals(ConfigProperties
                    .getString("ResourceComponentCallbacks.8"))) {
                ConnectorComponentCallbacks.logger.debug("Testing KV connector connector"); //$NON-NLS-1$
                try {
                    succeeded = true;
                } catch (final Throwable e) {
                    ExceptionTracer.traceIgnored(e);
                }
                reply = ComponentCallReply.create(true, Boolean.valueOf(succeeded),
                        ByteBuffer.allocate(0), request.reference);
                component.callReturn(reply);
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return null;
    }

    @Override
    public CallbackCompletion<Void> callReturned(final ComponentController component,
            final ComponentCallReply reply) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState(this.status == Status.Ready);
        if (this.pendingReferences.containsKey(reply.reference)) {
            final Trigger<ComponentCallReply> trigger = this.pendingReferences
                    .remove(reply.reference);
            trigger.triggerSucceeded(reply);
        } else {
            throw new IllegalStateException();
        }
        return null;
    }

    @Override
    public CallbackCompletion<Void> casted(final ComponentController component,
            final ComponentCastRequest request) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        throw new UnsupportedOperationException();
    }

    @Override
    public CallbackCompletion<Void> failed(final ComponentController component,
            final Throwable exception) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        this.component = null; // NOPMD by georgiana on 2/21/12 2:48 PM
        this.status = Status.Terminated;
        ExceptionTracer.traceIgnored(exception);
        return null;
    }

    @Override
    public void failedCallbacks(final Callbacks trigger, final Throwable exception) {
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
    public DeferredFuture<ComponentCallReply> findDriver(final ResourceType type) {
        ConnectorComponentCallbacks.logger.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
        Preconditions.checkState(this.status == Status.Ready);
        final ComponentCallReference callReference = ComponentCallReference.create();
        final DeferredFuture<ComponentCallReply> replyFuture = DeferredFuture
                .create(ComponentCallReply.class);
        ComponentIdentifier componentId = null; // NOPMD by georgiana on 2/21/12 2:47 PM
        switch (type) {
        case AMQP:
            componentId = this.amqpGroup;
            break;
        case KEY_VALUE:
            componentId = this.kvGroup;
            break;
        case MEMCACHED:
            componentId = this.mcGroup;
            break;
        default:
            break;
        }
        this.component.call(componentId, ComponentCallRequest.create(
                ConfigProperties.getString("ResourceComponentCallbacks.4"), null, callReference)); //$NON-NLS-1$
        this.pendingReferences.put(callReference, replyFuture.trigger);
        return replyFuture;
    }

    @Override
    public CallbackCompletion<Void> initialized(final ComponentController component) {
        Preconditions.checkState(this.component == null);
        Preconditions.checkState(this.status == Status.Created);
        this.component = component;
        final ComponentCallReference callReference = ComponentCallReference.create();
        this.component.register(this.selfGroup, callReference);
        final DeferredFuture<ComponentCallReply> result = DeferredFuture
                .create(ComponentCallReply.class);
        this.pendingReferences.put(callReference, result.trigger);
        this.status = Status.Unregistered;
        ConnectorComponentCallbacks.logger.trace("Connector component callback initialized."); //$NON-NLS-1$
        return null;
    }

    @Override
    public void registeredCallbacks(final Callbacks trigger, final CallbackIsolate isolate) { // NOPMD by georgiana on 2/21/12 2:47 PM
    }

    @Override
    public CallbackCompletion<Void> registerReturned(final ComponentController component,
            final ComponentCallReference reference, final boolean ok) {
        Preconditions.checkState(this.component == component);
        final Trigger<ComponentCallReply> pendingReply = this.pendingReferences.remove(reference);
        if (pendingReply != null) {
            if (!ok) {
                final Exception e = new Exception("failed registering to group; terminating!"); //$NON-NLS-1$
                ExceptionTracer.traceDeferred(e);
                this.component.terminate();
                throw new IllegalStateException(e);
            }
            ConnectorComponentCallbacks.logger
                    .info("Connector component callback registered to group " + this.selfGroup); //$NON-NLS-1$
            this.status = Status.Ready;
        } else {
            throw new IllegalStateException();
        }
        return null;
    }

    public void terminate() {
        Preconditions.checkState(this.component != null);
        this.component.terminate();
    }

    @Override
    public CallbackCompletion<Void> terminated(final ComponentController component) {
        Preconditions.checkState(this.component == component);
        Preconditions.checkState((this.status != Status.Terminated)
                && (this.status != Status.Unregistered));
        this.component = null; // NOPMD by georgiana on 2/21/12 2:46 PM
        this.status = Status.Terminated;
        ConnectorComponentCallbacks.logger.info("Connector component callback terminated."); //$NON-NLS-1$
        return null;
    }

    @Override // NOPMD by georgiana on 2/21/12 2:45 PM
    public void unregisteredCallbacks(final Callbacks trigger) {
    }

    private static void setComponentCallbacks(final ConnectorComponentCallbacks callbacks) {
        ConnectorComponentCallbacks.callbacks = callbacks;
    }

    public static ConnectorComponentCallbacks callbacks = null;
    private static MosaicLogger logger = MosaicLogger
            .createLogger(ConnectorComponentCallbacks.class);

    /**
     * Supported resource types.
     * 
     * @author Georgiana Macariu
     * 
     */
    public static enum ResourceType {
        // NOTE: MEMCACHED is not yet supported, but will be in the near future
        AMQP,
        KEY_VALUE,
        MEMCACHED;
    }

    static enum Status {
        Created, Ready, Terminated, Unregistered;
    }
}
