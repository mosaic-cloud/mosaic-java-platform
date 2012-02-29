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

package eu.mosaic_cloud.cloudlets.runtime.tests;


import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.runtime.Cloudlet;
import eu.mosaic_cloud.cloudlets.runtime.CloudletEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
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


public abstract class BaseCloudletTest<Scenario extends BaseCloudletTest.BaseScenario<Context>, Context extends Object>
{
	@Before
	public abstract void setUp ();
	
	@After
	public void tearDown ()
	{
		this.awaitSuccess (this.cloudlet.destroy ());
		this.scenario = null;
	}
	
	@Test
	public abstract void test ();
	
	protected void await (final CallbackCompletion<?> completion)
	{
		Assert.assertTrue (completion.await (this.scenario.poolTimeout));
	}
	
	protected boolean awaitBooleanOutcome (final CallbackCompletion<Boolean> completion)
	{
		this.await (completion);
		return this.getBooleanOutcome (completion);
	}
	
	protected <Outcome> Outcome awaitOutcome (final CallbackCompletion<Outcome> completion)
	{
		this.await (completion);
		return this.getOutcome (completion);
	}
	
	protected boolean awaitSuccess (final CallbackCompletion<?> completion)
	{
		this.await (completion);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return true;
	}
	
	protected boolean getBooleanOutcome (final CallbackCompletion<Boolean> completion)
	{
		final Boolean value = this.getOutcome (completion);
		Assert.assertNotNull (value);
		return value.booleanValue ();
	}
	
	protected <Outcome> Outcome getOutcome (final CallbackCompletion<Outcome> completion)
	{
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return completion.getOutcome ();
	}
	
	protected Cloudlet<Context> cloudlet;
	protected Scenario scenario;
	
	protected static <Scenario extends BaseScenario<Context>, Context extends Object> void setUpScenario (final Class<? extends BaseCloudletTest<Scenario, Context>> owner, final Scenario scenario, final String configuration, final Class<? extends ICloudletCallback<Context>> callbacksClass, final Class<Context> contextClass)
	{
		BasicThreadingSecurityManager.initialize ();
		scenario.logger = MosaicLogger.createLogger (owner);
		scenario.transcript = Transcript.create (owner);
		scenario.exceptions_ = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		scenario.exceptions = TranscriptExceptionTracer.create (scenario.transcript, scenario.exceptions_);
		if (configuration != null)
			scenario.configuration = PropertyTypeConfiguration.create (owner.getClassLoader (), configuration);
		else
			scenario.configuration = PropertyTypeConfiguration.create ();
		scenario.threading = BasicThreadingContext.create (owner, scenario.exceptions.catcher);
		scenario.threading.initialize ();
		scenario.reactor = BasicCallbackReactor.create (scenario.threading, scenario.exceptions);
		scenario.reactor.initialize ();
		scenario.callbacksClass = callbacksClass;
		scenario.contextClass = contextClass;
		scenario.environment = CloudletEnvironment.create (scenario.configuration, scenario.callbacksClass, scenario.contextClass, scenario.callbacksClass.getClassLoader (), scenario.reactor, scenario.threading, scenario.exceptions);
	}
	
	protected static void tearDownScenario (final BaseScenario<?> scenario)
	{
		Assert.assertTrue (scenario.reactor.destroy (scenario.poolTimeout));
		Assert.assertTrue (scenario.threading.destroy (scenario.poolTimeout));
	}
	
	protected static class BaseScenario<Context extends Object>
	{
		Class<Context> contextClass;
		Class<? extends ICloudletCallback<Context>> callbacksClass;
		IConfiguration configuration;
		TranscriptExceptionTracer exceptions;
		QueueingExceptionTracer exceptions_;
		MosaicLogger logger;
		long poolTimeout = 1000 * 1000;
		BasicCallbackReactor reactor;
		BasicThreadingContext threading;
		Transcript transcript;
		CloudletEnvironment environment;
	}
}
