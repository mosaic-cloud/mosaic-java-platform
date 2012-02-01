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

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.cloudlets.ConfigProperties;
import eu.mosaic_cloud.cloudlets.container.CloudletContainerPreMain.CloudletContainerParameters;
import eu.mosaic_cloud.cloudlets.core.CloudletException;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.idl.ChannelData;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonMapper;
import eu.mosaic_cloud.tools.miscellaneous.OutcomeFuture;
import eu.mosaic_cloud.tools.miscellaneous.OutcomeFuture.OutcomeTrigger;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;

/**
 * This callback class enables the container to communicate with other platform
 * components. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ContainerComponentCallbacks implements ComponentCallbacks,
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
	private static MosaicLogger logger = MosaicLogger
			.createLogger(ContainerComponentCallbacks.class);

	private Status status;
	private ComponentController component;
	private ThreadingContext threading;
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
		this.threading = Threading.getDefaultContext();
		this.pendingReferences = new IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>>();
		ContainerComponentCallbacks.callbacks = this;
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
		this.status = Status.Created;
	}

	@Override
	public CallbackReference called(ComponentController component,
			ComponentCallRequest request) {
		List<CloudletManager> containers = null;
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
					logger.debug("Loading cloudlet in JAR "
							+ operands.get(0) + " with configuration "
							+ operands.get(i));
					containers = startCloudlet(loader, operands.get(i)
							.toString());
					if (containers != null) {
						this.cloudletRunners.addAll(containers);
					}
				}
				ComponentCallReply reply = ComponentCallReply.create(true,
						Boolean.valueOf(true), ByteBuffer.allocate(0),
						request.reference);
				component.callReturn(reply);
				return null;
			}
			// else if (request.operation.equals(ConfigProperties
			//						.getString("ContainerComponentCallbacks.4"))) { //$NON-NLS-1$
			// logger.debug(
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
			else {
				throw new UnsupportedOperationException();
			}
		}
		throw new UnsupportedOperationException();
	}

	private List<CloudletManager> startCloudlet(ClassLoader loader,
			String configurationFile) {
		final IConfiguration configuration = PropertyTypeConfiguration.create(
				loader, configurationFile);
		if (configuration == null) {
			logger.error("Cloudlet configuration file " + configurationFile
					+ " is missing.");
			return null;
		}
		int noInstances = ConfigUtils.resolveParameter(configuration,
				ConfigProperties.getString("CloudletDummyContainer.3"),
				Integer.class, 1);
		List<CloudletManager> containers = new ArrayList<CloudletManager>();
		for (int i = 0; i < noInstances; i++) {
			final CloudletManager container = new CloudletManager(
					this.threading, loader, configuration);

			try {
				container.start();
				containers.add(container);
				logger.trace("Starting cloudlet with config file "
						+ configurationFile);
			} catch (CloudletException e) {
				ExceptionTracer.traceIgnored(e);
			}
		}
		return containers;
	}

	private ClassLoader getCloudletClassLoader(String classpathArgument) {
		final ClassLoader classLoader;
		if (classpathArgument != null) {
			final LinkedList<URL> classLoaderUrls = new LinkedList<URL>();
			for (final String classpathPart : classpathArgument.split(";")) {
				if (classpathPart.length() > 0) {
					final URL classpathUrl;
					if (classpathPart.startsWith("http:")
							|| classpathPart.startsWith("file:")) {
						try {
							classpathUrl = new URL(classpathPart);
						} catch (final Exception exception) {
							ExceptionTracer.traceDeferred(exception);
							throw (new IllegalArgumentException(String.format(
									"invalid class-path URL `%s`",
									classpathPart), exception));
						}
					} else {
						throw (new IllegalArgumentException(String.format(
								"invalid class-path URL `%s`", classpathPart)));
					}
					logger.trace("Loading cloudlet from " + classpathUrl
							+ "...");
					classLoaderUrls.add(classpathUrl);
				}
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
	public CallbackReference callReturned(ComponentController component,
			ComponentCallReply reply) {
		Preconditions.checkState(this.component == component);
		Preconditions.checkState(this.status == Status.Ready);
		if (this.pendingReferences.containsKey(reply.reference)) {
			OutcomeTrigger<ComponentCallReply> trigger = this.pendingReferences
					.remove(reply.reference);
			trigger.succeeded(reply);
		} else {
			throw (new IllegalStateException());
		}
		return null;
	}

	public void terminate() {
		Preconditions.checkState(this.component != null);
		// System.out.println("ContainerComponentCallbacks.terminate()");
		this.component.terminate();
	}

	@Override
	public CallbackReference casted(ComponentController component,
			ComponentCastRequest request) {
		Preconditions.checkState(this.component == component);
		Preconditions.checkState((this.status != Status.Terminated)
				&& (this.status != Status.Unregistered));
		throw (new UnsupportedOperationException());
	}

	@Override
	public CallbackReference failed(ComponentController component, Throwable exception) {
		logger.trace("ComponentController container failed " + exception.getMessage());
		Preconditions.checkState(this.component == component);
		Preconditions.checkState(this.status != Status.Terminated);
		Preconditions.checkState(this.status != Status.Unregistered);
		// also stop and destroy connector & cloudlets
		for (CloudletManager container : this.cloudletRunners) {
			container.stop();
		}
		this.component = null;
		this.status = Status.Terminated;
		ExceptionTracer.traceHandled(exception);
		return null;
	}

	@Override
	public CallbackReference initialized(ComponentController component) {
		Preconditions.checkState(this.component == null);
		Preconditions.checkState(this.status == Status.Created);
		this.component = component;
		this.status = Status.Unregistered;
		ComponentCallReference callReference = ComponentCallReference
				.create();
		this.component.register(this.selfGroup, callReference);
		OutcomeFuture<ComponentCallReply> result = OutcomeFuture.create();
		this.pendingReferences.put(callReference, result.trigger);
		logger.trace("Container component callback initialized."); //$NON-NLS-1$
		return null;
	}

	@Override
	public CallbackReference registerReturned(ComponentController component,
			ComponentCallReference reference, boolean ok) {
		Preconditions.checkState(this.component == component);
		OutcomeTrigger<ComponentCallReply> pendingReply = this.pendingReferences
				.remove(reference);
		if (pendingReply != null) {
			if (!ok) {
				Exception e = new Exception(
						"failed registering to group; terminating!"); //$NON-NLS-1$
				ExceptionTracer.traceDeferred(e);
				this.component.terminate();
				throw (new IllegalStateException(e));
			}
			this.status = Status.Ready;
			logger.info(
					"Container component callback registered to group " + this.selfGroup); //$NON-NLS-1$

			if (CloudletContainerParameters.configFile != null) {
				ClassLoader loader = getCloudletClassLoader(CloudletContainerParameters.classpath);
				List<CloudletManager> containers = startCloudlet(loader,
						CloudletContainerParameters.configFile);
				if (containers != null) {
					this.cloudletRunners.addAll(containers);
				}
			} else {
				logger.error("Missing config file");
			}
		} else {
			throw (new IllegalStateException());
		}
		return null;
	}

	@Override
	public CallbackReference terminated(ComponentController component) {
		logger.info("Container component callback terminating.");
		Preconditions.checkState(this.component == component);
		Preconditions.checkState(this.status != Status.Terminated);
		Preconditions.checkState(this.status != Status.Unregistered);
		// also stop and destroy connector & cloudlets
		for (CloudletManager container : this.cloudletRunners) {
			container.stop();
		}
		this.component = null;
		this.status = Status.Terminated;
		logger.info("Container component callback terminated."); //$NON-NLS-1$
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
	public ChannelData findDriver(ResourceType type) {
		logger.trace("Finding " + type.toString() + " driver"); //$NON-NLS-1$ //$NON-NLS-2$
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
				logger.debug("Found driver on channel " + channel);
			}
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
		}

		return channel;
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
