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


import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.runtime.Cloudlet;
import eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest.BaseScenario;

import org.junit.Test;

import junit.framework.Assert;


public abstract class BaseCloudletTest
		extends eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest<BaseScenario<?>>
{
	@Override
	@Test
	public void test ()
	{
		this.awaitSuccess (this.cloudlet.initialize ());
		Assert.assertTrue (this.cloudlet.await (this.scenario.poolTimeout));
		this.cloudlet = null;
	}
	
	protected <Context> void setUp (final Class<? extends ICloudletCallback<Context>> callbacksClass, final Class<Context> contextClass)
	{
		final BaseScenario<Context> scenario = new BaseScenario<Context> ();
		this.scenario = scenario;
		eu.mosaic_cloud.cloudlets.runtime.tests.BaseCloudletTest.setUpScenario (this.getClass (), scenario, null, callbacksClass, contextClass);
		this.cloudlet = Cloudlet.create (this.scenario.environment);
	}
}
