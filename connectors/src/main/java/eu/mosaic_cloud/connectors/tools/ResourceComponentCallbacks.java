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
public final class ResourceComponentCallbacks implements ComponentCallbacks,
		CallbackHandler {

	static enum Status {
		Created, Terminated, Unregistered, Ready;
	}

	/**
	 * Supported resource types.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	public static enum ResourceType {
		// NOTE: MEMCACHED is not yet supported, but will be in the near future
		AMQP, KEY_VALUE, MEMCACHED;
	}

	public static ResourceComponentCallbacks callbacks = null;

	private Status status;
	private ComponentController component;
	private IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> pendingReferences;
	private ComponentIdentifier amqpGroup;
	private ComponentIdentifier kvGroup;
	private ComponentIdentifier mcGroup;
	private ComponentIdentifier selfGroup;

	private static MosaicLogger logger = MosaicLogger
			.createLogger(ResourceComponentCallbacks.class);

	/**
	 * Creates a callback which is used by the mOSAIC platform to communicate
	 * with the connectors.
	 */
	public ResourceComponentCallbacks(ComponentContext context) {
		super();
		this.pendingReferences = new IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>>();
		ResourceComponentCallbacks.setComponentCallbacks(this);
		IConfiguration configuration = PropertyTypeConfiguration.create(
				ResourceComponentCallbacks.class.getClassLoader(),
				"resource-conn.properties"); //$NON-NLS-1$
		// set; // FIXME ?
		this.amqpGroup = ComponentIdentifier
				.resolve(ConfigUtils.resolveParameter(
						configuration,
						ConfigProperties
								.getString("ResourceComponentCallbacks.0"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
		this.kvGroup = ComponentIdentifier.resolve(ConfigUtils
				.resolveParameter(configuration, ConfigProperties
						.getString("ResourceComponentCallbacks.1"), //$NON-NLS-1$
						String.class, "")); //$NON-NLS-1$
		this.mcGroup = ComponentIdentifier
				.resolve(ConfigUtils.resolveParameter(
						configuration,
						ConfigProperties
								.getString("ResourceComponentCallbacks.2"), String.class, //$NON-NLS-1$
						"")); //$NON-NLS-1$
		this.selfGroup = ComponentIdentifier
				.resolve(ConfigUtils.resolveParameter(
						configuration,
						ConfigProperties
								.getString("ResourceComponentCallbacks.3"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
		this.status = Status.Created;
	}

	private static void setComponentCallbacks(
			ResourceComponentCallbacks callbacks) {
		ResourceComponentCallbacks.callbacks = callbacks;
	}

	@Override
	public CallbackCompletion<Void> called(ComponentController component,
			ComponentCallRequest request) {
		ComponentCallReply reply = null;
		boolean succeeded = false;
		Preconditions.checkState(this.component == component);
		Preconditions.checkState((this.status != Status.Terminated)
				&& (this.status != Status.Unregistered));
		if (this.status == Status.Ready) {
			if (request.operation.equals(ConfigProperties
					.getString("ResourceComponentCallbacks.7"))) { //$NON-NLS-1$
				logger.debug("Testing AMQP connector"); //$NON-NLS-1$
				try {
					// !!!!
					// AmqpConnectorCompTest.test();
					succeeded = true;
				} catch (Throwable e) {
					ExceptionTracer.traceIgnored(e);
				}
				reply = ComponentCallReply.create(true,
						Boolean.valueOf(succeeded), ByteBuffer.allocate(0),
						request.reference);
				component.callReturn(reply);
			} else if (request.operation.equals(ConfigProperties
					.getString("ResourceComponentCallbacks.8"))) {
				logger.debug("Testing KV connector connector"); //$NON-NLS-1$
				try {
					// !!!!
					// KeyValueConnectorCompTest.test();
					succeeded = true;
				} catch (Throwable e) {
					ExceptionTracer.traceIgnored(e);
				}
				reply = ComponentCallReply.create(true,
						Boolean.valueOf(succeeded), ByteBuffer.allocate(0),
						request.reference);
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
	public CallbackCompletion<Void> callReturned(ComponentController component,
			ComponentCallReply reply) {
		Preconditions.checkState(this.component == component);
		Preconditions.checkState(this.status == Status.Ready);
		if (this.pendingReferences.containsKey(reply.reference)) {
			Trigger<ComponentCallReply> trigger = this.pendingReferences
					.remove(reply.reference);
			trigger.triggerSucceeded(reply);
		} else {
			throw (new IllegalStateException());
		}
		return null;
	}

	public void terminate() {
		Preconditions.checkState(this.component != null);
		this.component.terminate();
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
		Preconditions.checkState(this.component == component);
		Preconditions.checkState((this.status != Status.Terminated)
				&& (this.status != Status.Unregistered));
		this.component = null;
		this.status = Status.Terminated;
		ExceptionTracer.traceIgnored(exception);
		return null;
	}

	@Override
	public CallbackCompletion<Void> initialized(ComponentController component) {
		Preconditions.checkState(this.component == null);
		Preconditions.checkState(this.status == Status.Created);
		this.component = component;
		ComponentCallReference callReference = ComponentCallReference
				.create();
		this.component.register(this.selfGroup, callReference);
		DeferredFuture<ComponentCallReply> result = DeferredFuture.create(ComponentCallReply.class);
		this.pendingReferences.put(callReference, result.trigger);
		this.status = Status.Unregistered;
		logger.trace("Connector component callback initialized."); //$NON-NLS-1$
		return null;
	}

	@Override
	public CallbackCompletion<Void> registerReturned(ComponentController component,
			ComponentCallReference reference, boolean ok) {
		Preconditions.checkState(this.component == component);
		Trigger<ComponentCallReply> pendingReply = this.pendingReferences
				.remove(reference);
		if (pendingReply != null) {
			if (!ok) {
				Exception e = new Exception(
						"failed registering to group; terminating!"); //$NON-NLS-1$
				ExceptionTracer.traceDeferred(e);
				this.component.terminate();
				throw (new IllegalStateException(e));

			}
			logger.info("Connector component callback registered to group " + this.selfGroup); //$NON-NLS-1$
			this.status = Status.Ready;
		} else {
			throw (new IllegalStateException());
		}
		return null;
	}

	@Override
	public CallbackCompletion<Void> terminated(ComponentController component) {
		Preconditions.checkState(this.component == component);
		Preconditions.checkState((this.status != Status.Terminated)
				&& (this.status != Status.Unregistered));
		this.component = null;
		this.status = Status.Terminated;
		logger.info("Connector component callback terminated."); //$NON-NLS-1$
		return null;
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
	public DeferredFuture<ComponentCallReply> findDriver(ResourceType type) {
		logger.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
		Preconditions.checkState(this.status == Status.Ready);

		ComponentCallReference callReference = ComponentCallReference.create();
		DeferredFuture<ComponentCallReply> replyFuture = DeferredFuture.create(ComponentCallReply.class);
		ComponentIdentifier componentId = null;
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
		this.component
				.call(componentId,
						ComponentCallRequest.create(
								ConfigProperties
										.getString("ResourceComponentCallbacks.4"), null, callReference)); //$NON-NLS-1$
		this.pendingReferences.put(callReference, replyFuture.trigger);

		return replyFuture;
	}

	@Override
	public void registeredCallbacks(Callbacks trigger, CallbackIsolate isolate) {
	}

	@Override
	public void unregisteredCallbacks(Callbacks trigger) {
	}

	@Override
	public void failedCallbacks(Callbacks trigger, Throwable exception) {
	}
}
