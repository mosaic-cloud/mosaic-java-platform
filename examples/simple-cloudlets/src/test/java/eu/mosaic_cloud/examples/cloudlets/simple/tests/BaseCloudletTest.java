/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple.tests;


import java.util.UUID;

import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.runtime.Cloudlet;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.drivers.interop.queue.amqp.AmqpStub;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;

import org.junit.After;
import org.junit.Test;

import com.google.common.base.Preconditions;

import junit.framework.Assert;


public abstract class BaseCloudletTest
		extends eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest<BaseCloudletTest.Scenario<?>>
{
	@Override
	@After
	public void tearDown ()
	{
		if (this.scenario.amqpDriverStub != null)
			this.scenario.amqpDriverStub.destroy ();
		if (this.scenario.kvDriverStub != null)
			this.scenario.kvDriverStub.destroy ();
		if (this.scenario.driversChannel != null)
			this.scenario.driversChannel.terminate ();
		if (this.scenario.connectorsChannel != null)
			this.scenario.connectorsChannel.terminate ();
		if (this.cloudlet != null)
			this.awaitSuccess (this.cloudlet.destroy ());
		eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest.tearDownScenario (this.scenario);
		this.cloudlet = null;
		this.scenario = null;
	}
	
	@Override
	@Test
	public void test ()
	{
		this.awaitSuccess (this.cloudlet.initialize ());
		Assert.assertTrue (this.cloudlet.await (this.scenario.poolTimeout));
		this.cloudlet = null;
	}
	
	protected <Context> void setUp (final Class<? extends ICloudletCallback<Context>> callbacksClass, final Class<Context> contextClass, final String configuration)
	{
		final Scenario<Context> scenario = new Scenario<Context> ();
		this.scenario = scenario;
		final ChannelFactory connectorsChannelFactory = new ChannelFactory () {
			@Override
			public Channel create ()
			{
				Preconditions.checkState (scenario.connectorsChannel != null);
				Preconditions.checkState (scenario.driversChannel != null);
				return (scenario.connectorsChannel);
			}
		};
		final ChannelResolver connectorsChannelResolver = new ChannelResolver () {
			@Override
			public void resolve (final String target, final ResolverCallbacks callbacks)
			{
				Preconditions.checkNotNull (target);
				Preconditions.checkNotNull (callbacks);
				Preconditions.checkState (scenario.connectorsChannel != null);
				Preconditions.checkState (scenario.driversChannel != null);
				if ("a5e40f0b2c041bc694ace68ace08420d40f9cbc0".equals (target))
					callbacks.resolved (this, target, scenario.driversIdentity, scenario.driversEndpoint);
				else if ("a3e40f0b2c041bc694ace68ace08420d40f9cbc0".equals (target))
					callbacks.resolved (this, target, scenario.driversIdentity, scenario.driversEndpoint);
				else
					throw (new IllegalArgumentException ());
			}
		};
		eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest.setUpScenario (this.getClass (), scenario, configuration, callbacksClass, contextClass, connectorsChannelFactory, connectorsChannelResolver);
		{
			scenario.connectorsIdentity = UUID.randomUUID ().toString ();
			scenario.connectorsChannel = ZeroMqChannel.create (scenario.connectorsIdentity, scenario.threading, scenario.exceptions);
		}
		{
			scenario.driversIdentity = UUID.randomUUID ().toString ();
			scenario.driversEndpoint = "inproc://" + scenario.driversIdentity;
			scenario.driversChannel = ZeroMqChannel.create (scenario.driversIdentity, scenario.threading, scenario.exceptions);
			scenario.driversChannel.accept (scenario.driversEndpoint);
		}
		{
			final PropertyTypeConfiguration driverConfiguration = PropertyTypeConfiguration.create (this.getClass ().getClassLoader (), "amqp-queue-driver-test.properties");
			scenario.driversChannel.register (AmqpSession.DRIVER);
			scenario.amqpDriverStub = AmqpStub.create (driverConfiguration, scenario.driversChannel, scenario.threading);
		}
		{
			final PropertyTypeConfiguration driverConfiguration = PropertyTypeConfiguration.create (this.getClass ().getClassLoader (), "riak-http-kv-store-driver-test.properties");
			scenario.driversChannel.register (KeyValueSession.DRIVER);
			scenario.kvDriverStub = KeyValueStub.create (driverConfiguration, scenario.threading, scenario.driversChannel);
		}
		this.cloudlet = Cloudlet.create (this.scenario.environment);
	}
	
	public static class Scenario<Context extends Object>
			extends eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest.BaseScenario<Context>
	{
		public AbstractDriverStub amqpDriverStub;
		public ZeroMqChannel connectorsChannel;
		public String connectorsIdentity;
		public ZeroMqChannel driversChannel;
		public String driversEndpoint;
		public String driversIdentity;
		public AbstractDriverStub kvDriverStub;
	}
}
