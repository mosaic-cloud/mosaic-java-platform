package mosaic.connector.components;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

import mosaic.connector.ConfigProperties;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.tools.OutcomeFuture;
import eu.mosaic_cloud.tools.OutcomeFuture.OutcomeTrigger;

/**
 * This callback class enables the connectors to find resource drivers and
 * connect to them. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ResourceComponentCallbacks implements ComponentCallbacks,
		CallbackHandler<ComponentCallbacks> {

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
	private Component component;
	private Monitor monitor;
	private IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>> pendingReferences;
	private ComponentIdentifier amqpGroup;
	private ComponentIdentifier kvGroup;
	private ComponentIdentifier mcGroup;
	private ComponentIdentifier selfGroup;

	/**
	 * Creates a callback which is used by the mOSAIC platform to communicate
	 * with the connectors.
	 */
	public ResourceComponentCallbacks() {
		super();
		this.monitor = Monitor.create(this);
		this.pendingReferences = new IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>>();
		ResourceComponentCallbacks.setComponentCallbacks(this);
		// try {
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
		synchronized (this) {
			this.status = Status.Created;
		}
		// } catch (Throwable e) {
		// e.printStackTrace(System.err);
		// }
	}

	private static void setComponentCallbacks(
			ResourceComponentCallbacks callbacks) {
		ResourceComponentCallbacks.callbacks = callbacks;
	}

	@Override
	public CallbackReference called(Component component,
			ComponentCallRequest request) {
		ComponentCallReply reply = null;
		boolean succeeded = false;

		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			if (this.status == Status.Ready) {
				if (request.operation.equals(ConfigProperties
						.getString("ResourceComponentCallbacks.7"))) { //$NON-NLS-1$
					MosaicLogger.getLogger().debug("Testing AMQP connector"); //$NON-NLS-1$
					try {
						AmqpConnectorCompTest.test();
						succeeded = true;

					} catch (Throwable e) {
						ExceptionTracer.traceDeferred(e);
					}
					reply = ComponentCallReply.create(true,
							Boolean.valueOf(succeeded), ByteBuffer.allocate(0),
							request.reference);
					component.reply(reply);
				} else if (request.operation.equals(ConfigProperties
						.getString("ResourceComponentCallbacks.8"))) {
					MosaicLogger.getLogger().debug(
							"Testing KV connector connector"); //$NON-NLS-1$
					try {
						KeyValueConnectorCompTest.test();
						succeeded = true;
					} catch (Throwable e) {
						ExceptionTracer.traceDeferred(e);
					}
					reply = ComponentCallReply.create(true,
							Boolean.valueOf(succeeded), ByteBuffer.allocate(0),
							request.reference);
					component.reply(reply);
				} else
					throw new UnsupportedOperationException();
			} else
				throw new UnsupportedOperationException();
		}
		return null;
	}

	@Override
	public CallbackReference callReturned(Component component,
			ComponentCallReply reply) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState(this.status == Status.Ready);
			if (this.pendingReferences.containsKey(reply.reference)) {
				OutcomeTrigger<ComponentCallReply> trigger = this.pendingReferences
						.remove(reply.reference);
				trigger.succeeded(reply);
			} else
				throw (new IllegalStateException());

		}
		return null;
	}

	public void terminate() {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component != null);
			this.component.terminate();
		}
	}

	@Override
	public CallbackReference casted(Component component,
			ComponentCastRequest request) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			throw (new UnsupportedOperationException());
		}
	}

	@Override
	public CallbackReference failed(Component component, Throwable exception) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			this.component = null;
			this.status = Status.Terminated;
			ExceptionTracer.traceIgnored(exception);
		}
		return null;
	}

	@Override
	public CallbackReference initialized(Component component) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == null);
			Preconditions.checkState(this.status == Status.Created);
			this.component = component;
			ComponentCallReference callReference = ComponentCallReference
					.create();
			this.component.register(this.selfGroup, callReference);
			OutcomeFuture<ComponentCallReply> result = OutcomeFuture.create();
			this.pendingReferences.put(callReference, result.trigger);
			this.status = Status.Unregistered;
			MosaicLogger.getLogger().trace(
					"Connector component callback initialized."); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public CallbackReference registerReturn(Component component,
			ComponentCallReference reference, boolean ok) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			OutcomeTrigger<ComponentCallReply> pendingReply = this.pendingReferences
					.remove(reference);
			if (pendingReply != null) {
				if (!ok) {
					ExceptionTracer.traceHandled(new Exception(
							"failed registering to group; terminating!")); //$NON-NLS-1$
					this.component.terminate();
					throw (new IllegalStateException());
				}
				MosaicLogger
						.getLogger()
						.info("Connector component callback registered to group " + this.selfGroup); //$NON-NLS-1$
				this.status = Status.Ready;
			} else
				throw (new IllegalStateException());
		}
		return null;
	}

	@Override
	public CallbackReference terminated(Component component) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			this.component = null;
			this.status = Status.Terminated;
			MosaicLogger.getLogger().info(
					"Connector component callback terminated."); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void deassigned(ComponentCallbacks trigger,
			ComponentCallbacks newCallbacks) {
	}

	@Override
	public void reassigned(ComponentCallbacks trigger,
			ComponentCallbacks oldCallbacks) {
	}

	@Override
	public void registered(ComponentCallbacks trigger) {
	}

	@Override
	public void unregistered(ComponentCallbacks trigger) {
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
	public OutcomeFuture<ComponentCallReply> findDriver(ResourceType type) {
		MosaicLogger.getLogger()
				.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
		Preconditions.checkState(this.status == Status.Ready);

		ComponentCallReference callReference = ComponentCallReference.create();
		OutcomeFuture<ComponentCallReply> replyFuture = OutcomeFuture.create();
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

}
