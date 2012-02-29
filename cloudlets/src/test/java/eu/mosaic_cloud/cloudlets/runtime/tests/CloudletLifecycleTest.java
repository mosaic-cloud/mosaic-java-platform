
package eu.mosaic_cloud.cloudlets.runtime.tests;


import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.runtime.Cloudlet;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class CloudletLifecycleTest
		extends BaseCloudletTest<BaseCloudletTest.BaseScenario<VoidCloudletContext>, VoidCloudletContext>
{
	@Override
	public void setUp ()
	{
		this.scenario = new BaseScenario<VoidCloudletContext> ();
		BaseCloudletTest.setUpScenario (this.getClass (), this.scenario, null, Callbacks.class, VoidCloudletContext.class);
		this.cloudlet = Cloudlet.create (this.scenario.environment);
	}
	
	@Override
	public void test ()
	{
		this.awaitSuccess (this.cloudlet.initialize ());
	}
	
	public static class Callbacks
			extends DefaultCloudletCallback<VoidCloudletContext>
	{
		@Override
		public CallbackCompletion<Void> destroy (final VoidCloudletContext context, final CloudletCallbackArguments<VoidCloudletContext> arguments)
		{
			this.logger.debug ("destroying");
			return (ICallback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final VoidCloudletContext context, final CloudletCallbackArguments<VoidCloudletContext> arguments)
		{
			this.logger.debug ("initializing");
			return (ICallback.SUCCESS);
		}
	}
}
