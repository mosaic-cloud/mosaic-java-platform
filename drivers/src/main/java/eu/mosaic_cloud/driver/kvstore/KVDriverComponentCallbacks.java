/*
 * #%L
 * mosaic-drivers
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.driver.kvstore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


import com.google.common.base.Preconditions;

import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.driver.AbstractDriverComponentCallbacks;
import eu.mosaic_cloud.driver.ConfigProperties;
import eu.mosaic_cloud.driver.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.tools.Monitor;

/**
 * This callback class enables the Key Value store driver to be exposed as a
 * component. Upon initialization it will look for a Key Value store server and
 * will create a driver object for the server.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class KVDriverComponentCallbacks extends
		AbstractDriverComponentCallbacks {

	private String driverName;

	/**
	 * Creates a driver callback.
	 */
	public KVDriverComponentCallbacks() {
		super();
		this.monitor = Monitor.create(this);
		try {
			IConfiguration configuration = PropertyTypeConfiguration
					.create(KVDriverComponentCallbacks.class
							.getResourceAsStream("kv.properties"));
			setDriverConfiguration(configuration);
			this.resourceGroup = ComponentIdentifier.resolve(ConfigUtils
					.resolveParameter(getDriverConfiguration(),
							ConfigProperties
									.getString("KVDriverComponentCallbacks.0"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$
			this.selfGroup = ComponentIdentifier.resolve(ConfigUtils
					.resolveParameter(getDriverConfiguration(),
							ConfigProperties
									.getString("KVDriverComponentCallbacks.1"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$
			this.driverName = ConfigUtils.resolveParameter(
					getDriverConfiguration(),
					ConfigProperties.getString("KVStoreDriver.6"), //$NON-NLS-1$
					String.class, ""); //$NON-NLS-1$

			this.status = Status.Created;
		} catch (IOException e) {
			ExceptionTracer.traceIgnored(e);
		}
	}

	@Override
	public CallbackReference called(Component component,
			ComponentCallRequest request) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions
					.checkState((this.status != KVDriverComponentCallbacks.Status.Terminated)
							&& (this.status != KVDriverComponentCallbacks.Status.Unregistered));
			if (this.status == KVDriverComponentCallbacks.Status.Registered) {
				if (request.operation.equals(ConfigProperties
						.getString("KVDriverComponentCallbacks.5"))) { //$NON-NLS-1$
					String channelEndpoint = ConfigUtils.resolveParameter(
							getDriverConfiguration(), ConfigProperties
									.getString("KVDriverComponentCallbacks.3"), //$NON-NLS-1$
							String.class, "");
					// FIXME
					try {
						if (System.getenv("mosaic_node_ip") != null)
							channelEndpoint = channelEndpoint.replace(
									"0.0.0.0", System.getenv("mosaic_node_ip"));
						else
							channelEndpoint = channelEndpoint.replace(
									"0.0.0.0", InetAddress.getLocalHost()
											.getHostAddress());
					} catch (UnknownHostException e) {
						ExceptionTracer.traceIgnored(e);
					}
					String channelId = ConfigUtils.resolveParameter(
							getDriverConfiguration(), ConfigProperties
									.getString("KVDriverComponentCallbacks.4"), //$NON-NLS-1$
							String.class, "");
					Map<String, String> outcome = new HashMap<String, String>();
					outcome.put("channelEndpoint", channelEndpoint);
					outcome.put("channelIdentifier", channelId);
					ComponentCallReply reply = ComponentCallReply.create(true,
							outcome, ByteBuffer.allocate(0), request.reference);
					component.reply(reply);
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				throw new UnsupportedOperationException();
			}
			return null;
		}
	}

	@Override
	public CallbackReference callReturned(Component component,
			ComponentCallReply reply) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			if (this.pendingReference == reply.reference) {
				if (this.status == Status.WaitingResourceResolved) {
					//					this.pendingReference = null;
					String ipAddress;
					Integer port;
					try {
						Preconditions.checkArgument(reply.ok);
						Preconditions
								.checkArgument(reply.outputsOrError instanceof Map);
						final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
						MosaicLogger.getLogger().trace(
								"Resource search returned " + outputs);

						ipAddress = (String) outputs.get("ip"); //$NON-NLS-1$
						Preconditions.checkArgument(ipAddress != null);
						port = (Integer) outputs.get("port"); //$NON-NLS-1$
						Preconditions.checkArgument(port != null);
					} catch (IllegalArgumentException exception) {
						this.terminate();
						ExceptionTracer
								.traceDeferred(
										exception,
										"failed resolving Riak broker endpoint: `%s`; terminating!", //$NON-NLS-1$
										reply.outputsOrError);
						throw new IllegalStateException(exception);
					}
					MosaicLogger.getLogger().trace(
							"Resolved Riak on " + ipAddress + ":" //$NON-NLS-1$ //$NON-NLS-2$
									+ port);
					this.configureDriver(ipAddress, port.toString());
					if (this.selfGroup != null) {
						this.pendingReference = ComponentCallReference.create();
						this.status = Status.Unregistered;
						this.component.register(this.selfGroup,
								this.pendingReference);
					}
				} else {
					throw new IllegalStateException();
				}
			} else {
				throw new IllegalStateException();
			}
		}
		return null;
	}

	private void configureDriver(String brokerIp, String port) {
		getDriverConfiguration().addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("KVStoreDriver.0")), brokerIp); //$NON-NLS-1$
		getDriverConfiguration().addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("KVStoreDriver.1")), port); //$NON-NLS-1$

	}

	@Override
	public CallbackReference initialized(Component component) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == null);
			Preconditions.checkState(this.status == Status.Created);
			this.component = component;
			final ComponentCallReference callReference = ComponentCallReference
					.create();
			String operation;
			if (this.driverName
					.equalsIgnoreCase(KeyValueDriverFactory.DriverType.RIAKPB
							.toString())) {
				operation = ConfigProperties
						.getString("KVDriverComponentCallbacks.2");//$NON-NLS-1$
			} else {
				operation = ConfigProperties
						.getString("KVDriverComponentCallbacks.6");//$NON-NLS-1$
			}
			this.component
					.call(this.resourceGroup, ComponentCallRequest.create(
							operation, null, callReference));
			this.pendingReference = callReference;
			this.status = Status.WaitingResourceResolved;
			MosaicLogger.getLogger().trace(
					"Key Value driver callback initialized.");
		}
		return null;
	}

	@Override
	public CallbackReference registerReturn(Component component,
			ComponentCallReference reference, boolean success) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			if (this.pendingReference == reference) {
				//				this.pendingReference = null;
				if (!success) {
					Exception e = new Exception(
							"failed registering to group; terminating!"); //$NON-NLS-1$
					ExceptionTracer.traceDeferred(e);
					this.component.terminate();
					throw (new IllegalStateException(e));
				}
				MosaicLogger
						.getLogger()
						.info("Key Value Store driver callback registered to group " + this.selfGroup); //$NON-NLS-1$
				this.status = Status.Registered;

				// create stub and interop channel
				ZeroMqChannel driverChannel = createDriverChannel(
						ConfigProperties
								.getString("KVDriverComponentCallbacks.4"),
						ConfigProperties
								.getString("KVDriverComponentCallbacks.3"),
						KeyValueSession.DRIVER);
				this.stub = KeyValueStub.create(getDriverConfiguration(),
						driverChannel);

			} else {
				throw new IllegalStateException();
			}
		}
		return null;
	}

	@Override
	public void deassigned(ComponentCallbacks trigger,
			ComponentCallbacks newCallbacks) {
		// nothing to do here
	}

	@Override
	public void reassigned(ComponentCallbacks trigger,
			ComponentCallbacks oldCallbacks) {
		// nothing to do here
	}

	@Override
	public void registered(ComponentCallbacks trigger) {
		// nothing to do here
	}

	@Override
	public void unregistered(ComponentCallbacks trigger) {
		// nothing to do here
	}

}
