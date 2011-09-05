package mosaic.cloudlet.runtime;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import mosaic.cloudlet.CloudletContainerPreMain.CloudletContainerParameters;
import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.core.CloudletException;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.interop.idl.ChannelData;

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
import eu.mosaic_cloud.json.tools.DefaultJsonMapper;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.tools.OutcomeFuture;
import eu.mosaic_cloud.tools.OutcomeFuture.OutcomeTrigger;

/**
 * This callback class enables the container to communicate with other platform
 * components. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ContainerComponentCallbacks implements ComponentCallbacks,
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
		AMQP("queue"), KEY_VALUE("kvstore"), MEMCACHED("kvstore");
		public String getConfigPrefix() {
			return this.configPrefix;
		}

		private final String configPrefix;

		ResourceType(String configPrefix) {
			this.configPrefix = configPrefix;
		}
	}

	public static ContainerComponentCallbacks callbacks = null;

	private Status status;
	private Component component;
	private Monitor monitor;
	private IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>> pendingReferences;
	private ComponentIdentifier amqpGroup;
	private ComponentIdentifier kvGroup;
	private ComponentIdentifier mcGroup;
	private ComponentIdentifier selfGroup;
	private List<CloudletManager> cloudletRunners = new ArrayList<CloudletManager>();

	/**
	 * Creates a callback which is used by the mOSAIC platform to communicate
	 * with the connectors.
	 */
	public ContainerComponentCallbacks() {
		super();
		this.monitor = Monitor.create(this);
		this.pendingReferences = new IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>>();
		ContainerComponentCallbacks.callbacks = this;
//		try {
			IConfiguration configuration = PropertyTypeConfiguration.create(
					ContainerComponentCallbacks.class.getClassLoader(),
					"resource-container.properties"); //$NON-NLS-1$
			this.amqpGroup = ComponentIdentifier
					.resolve(ConfigUtils.resolveParameter(
							configuration,
							ConfigProperties
									.getString("ContainerComponentCallbacks.0"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
			this.kvGroup = ComponentIdentifier.resolve(ConfigUtils
					.resolveParameter(configuration, ConfigProperties
							.getString("ContainerComponentCallbacks.1"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$
			this.mcGroup = ComponentIdentifier
					.resolve(ConfigUtils.resolveParameter(
							configuration,
							ConfigProperties
									.getString("ContainerComponentCallbacks.2"), String.class, //$NON-NLS-1$
							"")); //$NON-NLS-1$
			this.selfGroup = ComponentIdentifier
					.resolve(ConfigUtils.resolveParameter(
							configuration,
							ConfigProperties
									.getString("ContainerComponentCallbacks.3"), String.class, "")); //$NON-NLS-1$ //$NON-NLS-2$
			synchronized (this) {
				this.status = Status.Created;
			}
//		} catch (Throwable e) {
//			e.printStackTrace(System.err);
//		}
	}

	@Override
	public CallbackReference called(Component component,
			ComponentCallRequest request) {
		CloudletManager container = null;
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			if (this.status == Status.Ready) {
				if (request.operation.equals(ConfigProperties
						.getString("ContainerComponentCallbacks.4"))) {
					// TODO
					List<?> operands = DefaultJsonMapper.defaultInstance
							.decode(request.inputs, List.class);
					ClassLoader loader = getCloudletClassLoader(operands.get(0)
							.toString());
					for (int i = 1; i < operands.size(); i++) {
						MosaicLogger.getLogger().debug(
								"Loading cloudlet in JAR " + operands.get(0)
										+ " with configuration "
										+ operands.get(i));
						container = startCloudlet(loader, operands.get(i)
								.toString());
						if (container != null) {
							this.cloudletRunners.add(container);
						}
					}
					ComponentCallReply reply = ComponentCallReply.create(true,
							new Boolean(true), ByteBuffer.allocate(0),
							request.reference);
					component.reply(reply);
					return null;
				}
				// else if (request.operation.equals(ConfigProperties
				//						.getString("ContainerComponentCallbacks.4"))) { //$NON-NLS-1$
				// MosaicLogger.getLogger().debug(
				// "mOSAIC container - running test cloudlets");
				// container = TestRunner.runHelloWorld();
				// if (container != null)
				// cloudletRunners.add(container);
				//
				// ComponentCallReply reply = ComponentCallReply.create(true,
				// new Boolean(true), ByteBuffer.allocate(0),
				// request.reference);
				// component.reply(reply);
				// return null;
				// }
				else
					throw new UnsupportedOperationException();
			}
			throw new UnsupportedOperationException();
		}
	}

	private CloudletManager startCloudlet(ClassLoader loader,
			String configurationFile) {
		final IConfiguration configuration = PropertyTypeConfiguration.create(
				loader, configurationFile);
		final CloudletManager container = new CloudletManager(loader,
				configuration);

		try {
			container.start();
		} catch (CloudletException e) {
			ExceptionTracer.traceDeferred(e);
		}
		return container;
	}

	private ClassLoader getCloudletClassLoader(String classpathArgument) {
		final ClassLoader classLoader;
		if (classpathArgument != null) {
			final LinkedList<URL> classLoaderUrls = new LinkedList<URL>();
			for (final String classpathPart : classpathArgument.split(";"))
				if (classpathPart.length() > 0) {
					final URL classpathUrl;
					if (classpathPart.startsWith("http:")
							|| classpathPart.startsWith("file:")) {
						try {
							classpathUrl = new URL(classpathPart);
						} catch (final Exception exception) {
							throw (new IllegalArgumentException(String.format(
									"invalid class-path URL `%s`",
									classpathPart), exception));
						}
					} else
						throw (new IllegalArgumentException(String.format(
								"invalid class-path URL `%s`", classpathPart)));
					classLoaderUrls.add(classpathUrl);
					System.out.println("classpathurl: " + classpathUrl);
				}
			classLoader = new URLClassLoader(
					classLoaderUrls.toArray(new URL[0]),
					ContainerComponentCallbacks.class.getClassLoader());
		} else {
			classLoader = ClassLoader.getSystemClassLoader();
		}
		return classLoader;
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
			// also stop and destroy connector & cloudlets
			for (CloudletManager container : this.cloudletRunners) {
				container.stop();
			}
			this.component = null;
			this.status = Status.Terminated;
			ExceptionTracer.traceDeferred(exception);
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
					"Container component callback initialized."); //$NON-NLS-1$
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
						.info("Container component callback registered to group " + this.selfGroup); //$NON-NLS-1$

				if ((CloudletContainerParameters.classpath != null)
						&& (CloudletContainerParameters.configFile != null)) {
					ClassLoader loader = getCloudletClassLoader(CloudletContainerParameters.classpath);

					CloudletManager container = startCloudlet(loader,
							CloudletContainerParameters.configFile);
					if (container != null) {
						this.cloudletRunners.add(container);
					}
					MosaicLogger.getLogger().debug(
							"Loaded cloudlet in JAR "
									+ CloudletContainerParameters.classpath
									+ " with configuration "
									+ CloudletContainerParameters.configFile);
				}
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
			// also stop and destroy connector & cloudlets
			for (CloudletManager container : this.cloudletRunners) {
				container.stop();
			}
			this.component = null;
			this.status = Status.Terminated;
			MosaicLogger.getLogger().info(
					"Container component callback terminated."); //$NON-NLS-1$
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
	public ChannelData findDriver(ResourceType type) {
		MosaicLogger.getLogger()
				.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
		Preconditions.checkState(this.status == Status.Ready);

		ComponentCallReference callReference = ComponentCallReference.create();
		OutcomeFuture<ComponentCallReply> replyFuture = OutcomeFuture.create();
		ComponentIdentifier componentId = null;
		ComponentCallReply reply;
		ChannelData channel = null;

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

		this.pendingReferences.put(callReference, replyFuture.trigger);
		this.component
				.call(componentId,
						ComponentCallRequest.create(
								ConfigProperties
										.getString("ContainerComponentCallbacks.7"), null, callReference)); //$NON-NLS-1$

		try {
			reply = replyFuture.get();
			if (reply.outputsOrError instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> outcome = (Map<String, String>) reply.outputsOrError;
				channel = new ChannelData(outcome.get("channelIdentifier"),
						outcome.get("channelEndpoint"));
				MosaicLogger.getLogger().debug(
						"Found driver on channel " + channel);
			}
		} catch (InterruptedException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ExecutionException e) {
			ExceptionTracer.traceDeferred(e);
		}

		return channel;
	}

}
