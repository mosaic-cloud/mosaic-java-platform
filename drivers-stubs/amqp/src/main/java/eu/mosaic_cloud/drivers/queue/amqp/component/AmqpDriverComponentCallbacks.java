/*
 * #%L
 * mosaic-drivers-stubs-amqp
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.queue.amqp.component;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.components.core.ComponentAcquireReply;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.drivers.component.AbstractDriverComponentCallbacks;
import eu.mosaic_cloud.drivers.queue.amqp.interop.AmqpStub;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import com.google.common.base.Preconditions;


/**
 * This callback class enables the AMQP driver to be exposed as a component.
 * Upon initialization it will look for a RabbitMQ server and will create a
 * driver object for the server.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpDriverComponentCallbacks
		extends AbstractDriverComponentCallbacks
{
	/**
	 * Creates a driver callback.
	 */
	public AmqpDriverComponentCallbacks (final ComponentEnvironment context)
	{
		super (context);
		try {
			final IConfiguration configuration = PropertyTypeConfiguration.create (AmqpDriverComponentCallbacks.class.getResourceAsStream ("driver-component.properties"));
			this.setDriverConfiguration (configuration);
			this.resourceGroup = ComponentIdentifier.resolve (ConfigUtils.resolveParameter (this.getDriverConfiguration (), ConfigProperties.AmqpDriverComponentCallbacks_0, String.class, ""));
			this.selfGroup = ComponentIdentifier.resolve (ConfigUtils.resolveParameter (this.getDriverConfiguration (), ConfigProperties.AmqpDriverComponentCallbacks_1, String.class, ""));
			this.status = Status.Created;
		} catch (final IOException e) {
			this.exceptions.traceIgnoredException (e);
		}
	}
	
	@Override
	public final CallbackCompletion<Void> acquireReturned (final ComponentController component, final ComponentAcquireReply reply)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
	{
		Preconditions.checkState (this.component == component);
		Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
		if (this.status == Status.Registered) {
			if (request.operation.equals (ConfigProperties.AmqpDriverComponentCallbacks_5)) {
				String channelEndpoint = ConfigUtils.resolveParameter (this.getDriverConfiguration (), ConfigProperties.AmqpDriverComponentCallbacks_3, String.class, "");
				// FIXME: These parameters should be determined through
				//-- component "resource acquire" operations.
				//-- Also this hack reduces the number of driver instances of
				//-- the same type to one per VM.
				try {
					if (System.getenv ("mosaic_node_ip") != null) {
						channelEndpoint = channelEndpoint.replace ("0.0.0.0", System.getenv ("mosaic_node_ip"));
					} else {
						channelEndpoint = channelEndpoint.replace ("0.0.0.0", InetAddress.getLocalHost ().getHostAddress ());
					}
				} catch (final UnknownHostException e) {
					this.exceptions.traceIgnoredException (e);
				}
				final String channelId = ConfigUtils.resolveParameter (this.getDriverConfiguration (), ConfigProperties.AmqpDriverComponentCallbacks_4, String.class, "");
				final Map<String, String> outcome = new HashMap<String, String> ();
				outcome.put ("channelEndpoint", channelEndpoint);
				outcome.put ("channelIdentifier", channelId);
				final ComponentCallReply reply = ComponentCallReply.create (true, outcome, ByteBuffer.allocate (0), request.reference);
				component.callReturn (reply);
			} else {
				throw new UnsupportedOperationException ();
			}
		} else {
			throw new UnsupportedOperationException ();
		}
		return null;
	}
	
	@Override
	public CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
	{
		Preconditions.checkState (this.component == component);
		if ((this.pendingReference == reply.reference) && (this.status == Status.WaitingResourceResolved)) {
			String rabbitmqTransport;
			String brokerIp;
			Integer brokerPort;
			String user;
			String password;
			String virtualHost;
			if (reply.ok && (reply.outputsOrError instanceof Map)) {
				final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
				rabbitmqTransport = (String) outputs.get ("transport");
				brokerIp = (String) outputs.get ("ip");
				brokerPort = (Integer) outputs.get ("port");
				if (!"tcp".equals (rabbitmqTransport) || (brokerIp == null) || (brokerPort == null)) {
					this.terminate ();
					this.logger.error ("failed resolving RabbitMQ broker endpoint: `" + reply.outputsOrError + "`; terminating!");
					throw new IllegalStateException ();
				}
				user = (String) outputs.get ("username");
				password = (String) outputs.get ("password");
				virtualHost = (String) outputs.get ("virtualHost");
				user = user != null ? user : "";
				password = password != null ? password : "";
				virtualHost = virtualHost != null ? virtualHost : "";
				this.logger.debug ("Resolved RabbitMQ on " + brokerIp + ":" + brokerPort + " user = " + user + " password = " + password + " vhost = " + virtualHost);
				this.configureDriver (brokerIp, brokerPort.toString (), user, password, virtualHost);
			}
			if (this.selfGroup != null) {
				this.pendingReference = ComponentCallReference.create ();
				this.status = Status.Unregistered;
				this.component.register (this.selfGroup, this.pendingReference);
			}
		} else {
			throw new IllegalStateException ();
		}
		return null;
	}
	
	@Override
	public CallbackCompletion<Void> initialized (final ComponentController component)
	{
		Preconditions.checkState (this.component == null);
		Preconditions.checkState (this.status == Status.Created);
		this.component = component;
		final ComponentCallReference callReference = ComponentCallReference.create ();
		this.pendingReference = callReference;
		this.status = Status.WaitingResourceResolved;
		this.component.call (this.resourceGroup, ComponentCallRequest.create (ConfigProperties.AmqpDriverComponentCallbacks_2, null, callReference));
		this.logger.trace ("AMQP driver callback initialized.");
		return null;
	}
	
	@Override
	public CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean registerOk)
	{
		Preconditions.checkState (this.component == component);
		if (this.pendingReference == reference) {
			if (!registerOk) {
				final Exception e = new Exception ("failed registering to group; terminating!");
				this.exceptions.traceDeferredException (e);
				this.component.terminate ();
				throw (new IllegalStateException (e));
			}
			this.logger.info ("AMQP driver callback registered to group " + this.selfGroup);
			this.status = Status.Registered;
			// NOTE: create stub and interop channel
			final String channelId = ConfigProperties.AmqpDriverComponentCallbacks_4;
			final String channelEndpoint = ConfigProperties.AmqpDriverComponentCallbacks_3;
			final ZeroMqChannel driverChannel = this.createDriverChannel (channelId, channelEndpoint, AmqpSession.DRIVER);
			this.stub = AmqpStub.create (this.getDriverConfiguration (), driverChannel, this.threading);
		} else {
			throw new IllegalStateException ();
		}
		return null;
	}
	
	private void configureDriver (final String brokerIp, final String port, final String user, final String pass, final String vHost)
	{
		try {
			this.getDriverConfiguration ().addParameter (ConfigurationIdentifier.resolveRelative (ConfigProperties.AmqpDriver_1), brokerIp);
			this.getDriverConfiguration ().addParameter (ConfigurationIdentifier.resolveRelative (ConfigProperties.AmqpDriver_2), port);
			this.getDriverConfiguration ().addParameter (ConfigurationIdentifier.resolveRelative (ConfigProperties.AmqpDriver_3), user);
			this.getDriverConfiguration ().addParameter (ConfigurationIdentifier.resolveRelative (ConfigProperties.AmqpDriver_4), pass);
			this.getDriverConfiguration ().addParameter (ConfigurationIdentifier.resolveRelative (ConfigProperties.AmqpDriver_5), vHost);
		} catch (final NullPointerException e) {
			this.exceptions.traceIgnoredException (e);
		}
	}
}
