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

package eu.mosaic_cloud.connectors.tests;


import java.util.UUID;

import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.Assert;


public class BaseScenario
{
	public BaseScenario (final Class<? extends BaseConnectorTest<?, ? extends BaseScenario>> owner, final IConfiguration configuration)
	{
		this.configuration = configuration;
		this.transcript = Transcript.create (owner);
		this.exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, this.exceptionsQueue);
		// create threading context for connector and driver
		this.threading = BasicThreadingContext.create (owner, this.exceptions, this.exceptions.catcher);
		this.threading.initialize ();
		// create callback reactor
		this.callbacks = BasicCallbackReactor.create (this.threading, this.exceptions);
		this.callbacks.initialize ();
		// set-up communication channel with the driver
		final String driverIdentity = ConfigUtils.resolveParameter (this.configuration, "interop.driver.identifier", String.class, "");
		final String driverEndpoint = ConfigUtils.resolveParameter (this.configuration, "interop.channel.address", String.class, "");
		this.connectorChannel = ZeroMqChannel.create (UUID.randomUUID ().toString (), this.threading, this.exceptions);
		this.driverChannel = ZeroMqChannel.create (driverIdentity, this.threading, this.exceptions);
		this.driverChannel.accept (driverEndpoint);
		final ChannelFactory channelFactory = new ChannelFactory () {
			@SuppressWarnings ("synthetic-access")
			@Override
			public final Channel create ()
			{
				return BaseScenario.this.connectorChannel;
			}
		};
		this.channelResolver = new ChannelResolver () {
			@Override
			public final void resolve (final String target, final ResolverCallbacks callbacks)
			{
				Assert.assertEquals (driverIdentity, target);
				callbacks.resolved (this, target, driverIdentity, driverEndpoint);
			}
		};
		this.environment = ConnectorEnvironment.create (this.callbacks, this.threading, this.exceptions, channelFactory, this.channelResolver);
		this.driverChannel.register (KeyValueSession.DRIVER);
	}
	
	public void destroy ()
	{
		Assert.assertTrue (this.driverChannel.terminate (this.poolTimeout));
		Assert.assertTrue (this.connectorChannel.terminate (this.poolTimeout));
		Assert.assertTrue (this.callbacks.destroy (this.poolTimeout));
		Assert.assertTrue (this.threading.destroy (this.poolTimeout));
		Assert.assertNull (this.exceptionsQueue.queue.poll ());
	}
	
	public IConfiguration getConfiguration ()
	{
		return this.configuration;
	}
	
	public ZeroMqChannel getDriverChannel ()
	{
		return this.driverChannel;
	}
	
	public ConnectorEnvironment getEnvironment ()
	{
		return this.environment;
	}
	
	public long getPoolTimeout ()
	{
		return this.poolTimeout;
	}
	
	public BasicThreadingContext getThreading ()
	{
		return this.threading;
	}
	
	public void registerDriverRole (final SessionSpecification sessionRole)
	{
		this.driverChannel.register (sessionRole);
	}
	
	private BasicCallbackReactor callbacks;
	private ChannelResolver channelResolver;
	private IConfiguration configuration;
	private ZeroMqChannel connectorChannel;
	private ZeroMqChannel driverChannel;
	private ConnectorEnvironment environment;
	private TranscriptExceptionTracer exceptions;
	private QueueingExceptionTracer exceptionsQueue;
	private final long poolTimeout = 1000;
	private BasicThreadingContext threading;
	private Transcript transcript;
}
