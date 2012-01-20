/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.drivers.queue.amqp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.drivers.AbstractDriverComponentCallbacks;
import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.drivers.interop.queue.amqp.AmqpStub;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.amqp.AmqpSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.tools.Threading;

/**
 * This callback class enables the AMQP driver to be exposed as a component.
 * Upon initialization it will look for a RabbitMQ server and will create a
 * driver object for the server.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpDriverComponentCallbacks extends
		AbstractDriverComponentCallbacks {

	/**
	 * Creates a driver callback.
	 */
	public AmqpDriverComponentCallbacks() {
		super(Threading.getCurrentContext());
		this.monitor = Monitor.create(this);
		try {
			IConfiguration configuration = PropertyTypeConfiguration
					.create(AmqpDriverComponentCallbacks.class
							.getResourceAsStream("amqp.properties"));
			setDriverConfiguration(configuration);
			this.resourceGroup = ComponentIdentifier
					.resolve(ConfigUtils.resolveParameter(
							getDriverConfiguration(),
							ConfigProperties
									.getString("AmqpDriverComponentCallbacks.0"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$
			this.selfGroup = ComponentIdentifier
					.resolve(ConfigUtils.resolveParameter(
							getDriverConfiguration(),
							ConfigProperties
									.getString("AmqpDriverComponentCallbacks.1"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$

			synchronized (this) {
				this.status = Status.Created;
			}
		} catch (IOException e) {
			ExceptionTracer.traceIgnored(e);
		}
	}

	@Override
	public CallbackReference called(Component component,
			ComponentCallRequest request) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			if (this.status == Status.Registered) {
				if (request.operation.equals(ConfigProperties
						.getString("AmqpDriverComponentCallbacks.5"))) { //$NON-NLS-1$
					String channelEndpoint = ConfigUtils
							.resolveParameter(
									getDriverConfiguration(),
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.3"), //$NON-NLS-1$
									String.class, "");
					// FIXME
					try {
						if (System.getenv("mosaic_node_ip") != null) {
							channelEndpoint = channelEndpoint.replace(
									"0.0.0.0", System.getenv("mosaic_node_ip"));
						} else {
							channelEndpoint = channelEndpoint.replace(
									"0.0.0.0", InetAddress.getLocalHost()
											.getHostAddress());
						}
					} catch (UnknownHostException e) {
						ExceptionTracer.traceIgnored(e);
					}
					String channelId = ConfigUtils
							.resolveParameter(
									getDriverConfiguration(),
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.4"), //$NON-NLS-1$
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
			if ((this.pendingReference == reply.reference)
					&& (this.status == Status.WaitingResourceResolved)) {
				//					this.pendingReference = null;
				String rabbitmqTransport;
				String brokerIp;
				Integer brokerPort;
				String user;
				String password;
				String virtualHost;

				if (reply.ok && (reply.outputsOrError instanceof Map)) {
					final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
					rabbitmqTransport = (String) outputs.get("transport"); //$NON-NLS-1$
					brokerIp = (String) outputs.get("ip"); //$NON-NLS-1$
					brokerPort = (Integer) outputs.get("port"); //$NON-NLS-1$
					if (!"tcp".equals(rabbitmqTransport) || (brokerIp == null)
							|| (brokerPort == null)) {
						this.terminate();
						MosaicLogger
								.getLogger()
								.error("failed resolving RabbitMQ broker endpoint: `" + reply.outputsOrError + "`; terminating!" //$NON-NLS-1$
								);
						throw new IllegalStateException();
					}

					user = (String) outputs.get("username"); //$NON-NLS-1$
					password = (String) outputs.get("password"); //$NON-NLS-1$
					virtualHost = (String) outputs.get("virtualHost"); //$NON-NLS-1$
					user = user != null ? user : "";
					password = password != null ? password : "";
					virtualHost = virtualHost != null ? virtualHost : "";

					MosaicLogger.getLogger().debug(
							"Resolved RabbitMQ on " + brokerIp + ":" //$NON-NLS-1$ //$NON-NLS-2$
									+ brokerPort + " user = " + user
									+ " password = " + password + " vhost = "
									+ virtualHost);
					this.configureDriver(brokerIp, brokerPort.toString(), user,
							password, virtualHost);
				}
				if (this.selfGroup != null) {
					this.pendingReference = ComponentCallReference.create();
					this.status = Status.Unregistered;
					this.component.register(this.selfGroup,
							this.pendingReference);
				}
			} else {
				throw new IllegalStateException();
			}
		}
		return null;
	}

	private void configureDriver(String brokerIp, String port, String user,
			String pass, String vHost) {
		try {
			getDriverConfiguration().addParameter(
					ConfigurationIdentifier.resolveRelative(ConfigProperties
							.getString("AmqpDriver.1")), brokerIp); //$NON-NLS-1$
			getDriverConfiguration().addParameter(
					ConfigurationIdentifier.resolveRelative(ConfigProperties
							.getString("AmqpDriver.2")), port); //$NON-NLS-1$
			getDriverConfiguration().addParameter(
					ConfigurationIdentifier.resolveRelative(ConfigProperties
							.getString("AmqpDriver.3")), user); //$NON-NLS-1$
			getDriverConfiguration().addParameter(
					ConfigurationIdentifier.resolveRelative(ConfigProperties
							.getString("AmqpDriver.4")), pass); //$NON-NLS-1$
			getDriverConfiguration().addParameter(
					ConfigurationIdentifier.resolveRelative(ConfigProperties
							.getString("AmqpDriver.5")), vHost); //$NON-NLS-1$
		} catch (NullPointerException e) { // NOPMD by georgiana on 10/12/11 4:37 PM
			ExceptionTracer.traceIgnored(e);
		}
	}

	@Override
	public CallbackReference initialized(Component component) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == null);
			Preconditions.checkState(this.status == Status.Created);
			this.component = component;
			final ComponentCallReference callReference = ComponentCallReference
					.create();
			this.component.call(this.resourceGroup, ComponentCallRequest
					.create(ConfigProperties
							.getString("AmqpDriverComponentCallbacks.2"), null, //$NON-NLS-1$
							callReference));
			this.pendingReference = callReference;
			this.status = Status.WaitingResourceResolved;
			MosaicLogger.getLogger().trace("AMQP driver callback initialized.");
		}
		return null;
	}

	@Override
	public CallbackReference registerReturn(Component component,
			ComponentCallReference reference, boolean registerOk) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			if (this.pendingReference == reference) {
				//				this.pendingReference = null;
				if (!registerOk) {
					Exception e = new Exception(
							"failed registering to group; terminating!"); //$NON-NLS-1$
					ExceptionTracer.traceDeferred(e);
					this.component.terminate();
					throw (new IllegalStateException(e));
				}
				MosaicLogger
						.getLogger()
						.info("AMQP driver callback registered to group " + this.selfGroup); //$NON-NLS-1$
				this.status = Status.Registered;

				// create stub and interop channel
				String channelId = ConfigProperties
						.getString("AmqpDriverComponentCallbacks.4");
				String channelEndpoint = ConfigProperties
						.getString("AmqpDriverComponentCallbacks.3");
				ZeroMqChannel driverChannel = createDriverChannel(channelId,
						channelEndpoint, AmqpSession.DRIVER);

				this.stub = AmqpStub.create(getDriverConfiguration(),
						driverChannel, this.threading);
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
