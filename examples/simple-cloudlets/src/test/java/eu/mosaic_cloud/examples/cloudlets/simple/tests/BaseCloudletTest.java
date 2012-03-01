
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
