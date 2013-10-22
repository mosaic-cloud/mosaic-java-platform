/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.tests;


import java.io.IOException;

import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.platform.implementation.v2.cloudlets.core.Cloudlet;
import eu.mosaic_cloud.platform.implementation.v2.cloudlets.core.CloudletEnvironment;
import eu.mosaic_cloud.platform.implementation.v2.connectors.tools.DefaultConnectorsFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletCallback;
import eu.mosaic_cloud.platform.v2.connectors.component.ComponentConnector;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.implementations.basic.PropertiesBackedConfigurationSource;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public abstract class BaseCloudletTest<Scenario extends BaseCloudletTest.BaseScenario<?>>
{
	@Before
	public abstract void setUp ();
	
	@After
	public void tearDown () {
		if (this.cloudlet != null) {
			this.awaitSuccess (this.cloudlet.destroy ());
		}
		BaseCloudletTest.tearDownScenario (this.scenario);
		this.cloudlet = null;
		this.scenario = null;
	}
	
	@Test
	public abstract void test ();
	
	protected void await (final CallbackCompletion<?> completion) {
		Assert.assertTrue (completion.await (this.scenario.poolTimeout));
	}
	
	protected boolean awaitBooleanOutcome (final CallbackCompletion<Boolean> completion) {
		this.await (completion);
		return this.getBooleanOutcome (completion);
	}
	
	protected <Outcome> Outcome awaitOutcome (final CallbackCompletion<Outcome> completion) {
		this.await (completion);
		return this.getOutcome (completion);
	}
	
	protected boolean awaitSuccess (final CallbackCompletion<?> completion) {
		this.await (completion);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return true;
	}
	
	protected boolean getBooleanOutcome (final CallbackCompletion<Boolean> completion) {
		final Boolean value = this.getOutcome (completion);
		Assert.assertNotNull (value);
		return value.booleanValue ();
	}
	
	protected <Outcome> Outcome getOutcome (final CallbackCompletion<Outcome> completion) {
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return completion.getOutcome ();
	}
	
	protected Cloudlet<?> cloudlet;
	protected Scenario scenario;
	
	protected static <Scenario extends BaseScenario<Context>, Context extends Object> void setUpScenario (final Class<? extends BaseCloudletTest<?>> owner, final Scenario scenario, final String configuration, final Class<? extends CloudletCallback<Context>> callbacksClass, final Class<Context> contextClass) {
		final ComponentConnector componentConnector = new ComponentConnector () {
			@Override
			public CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource) {
				throw (new UnsupportedOperationException ());
			}
			
			@Override
			public <TInputs, TOutputs> CallbackCompletion<TOutputs> call (final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputs) {
				throw (new UnsupportedOperationException ());
			}
			
			@Override
			public <TInputs> CallbackCompletion<Void> cast (final ComponentIdentifier component, final String operation, final TInputs inputs) {
				throw (new UnsupportedOperationException ());
			}
			
			@Override
			public CallbackCompletion<Void> destroy () {
				throw (new UnsupportedOperationException ());
			}
			
			@Override
			public CallbackCompletion<Void> initialize () {
				throw (new UnsupportedOperationException ());
			}
		};
		final ChannelFactory connectorChannelFactory = new ChannelFactory () {
			@Override
			public Channel create () {
				throw (new UnsupportedOperationException ());
			}
		};
		final ChannelResolver connectorChannelResolver = new ChannelResolver () {
			@Override
			public void resolve (final String target, final ResolverCallbacks callbacks) {
				throw (new UnsupportedOperationException ());
			}
		};
		BaseCloudletTest.setUpScenario (owner, scenario, configuration, callbacksClass, contextClass, componentConnector, connectorChannelFactory, connectorChannelResolver);
	}
	
	protected static <Scenario extends BaseScenario<Context>, Context extends Object> void setUpScenario (final Class<? extends BaseCloudletTest<?>> owner, final Scenario scenario, final String configuration, final Class<? extends CloudletCallback<Context>> callbacksClass, final Class<Context> contextClass, final ComponentConnector componentConnector, final ChannelFactory connectorChannelFactory, final ChannelResolver connectorChannelResolver) {
		BasicThreadingSecurityManager.initialize ();
		scenario.transcript = Transcript.create (owner);
		scenario.exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		scenario.exceptions = TranscriptExceptionTracer.create (scenario.transcript, scenario.exceptionsQueue);
		if (configuration != null) {
			try {
				scenario.configuration = PropertiesBackedConfigurationSource.load (owner.getClassLoader ().getResourceAsStream (configuration));
			} catch (final IOException exception) {
				throw (new Error (exception));
			}
		} else {
			scenario.configuration = PropertiesBackedConfigurationSource.create ();
		}
		scenario.threading = BasicThreadingContext.create (owner, scenario.exceptions, scenario.exceptions.catcher);
		scenario.threading.initialize ();
		scenario.reactor = BasicCallbackReactor.create (scenario.threading, scenario.exceptions);
		scenario.reactor.initialize ();
		scenario.callbacksClass = callbacksClass;
		scenario.contextClass = contextClass;
		final ConnectorEnvironment connectorEnvironment = ConnectorEnvironment.create (scenario.reactor, scenario.threading, scenario.exceptions, connectorChannelFactory, connectorChannelResolver);
		scenario.connectors = DefaultConnectorsFactory.create (connectorEnvironment, null);
		scenario.environment = CloudletEnvironment.create (scenario.configuration, scenario.callbacksClass, scenario.contextClass, scenario.callbacksClass.getClassLoader (), scenario.connectors, connectorEnvironment, componentConnector, scenario.reactor, scenario.threading, scenario.exceptions);
	}
	
	protected static void tearDownScenario (final BaseScenario<?> scenario) {
		Assert.assertTrue (scenario.reactor.destroy (scenario.poolTimeout));
		Assert.assertTrue (scenario.threading.destroy (scenario.poolTimeout));
	}
	
	public static class BaseScenario<Context extends Object>
	{
		public Class<? extends CloudletCallback<Context>> callbacksClass;
		public ConfigurationSource configuration;
		public ConnectorsFactory connectors;
		public Class<Context> contextClass;
		public CloudletEnvironment environment;
		public TranscriptExceptionTracer exceptions;
		public QueueingExceptionTracer exceptionsQueue;
		public long poolTimeout = 60 * 1000;
		public BasicCallbackReactor reactor;
		public BasicThreadingContext threading;
		public Transcript transcript;
	}
}
