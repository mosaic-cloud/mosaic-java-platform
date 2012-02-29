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
