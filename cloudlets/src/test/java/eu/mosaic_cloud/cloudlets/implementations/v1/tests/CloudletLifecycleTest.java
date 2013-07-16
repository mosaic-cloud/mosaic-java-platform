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

package eu.mosaic_cloud.cloudlets.implementations.v1.tests;


import eu.mosaic_cloud.cloudlets.implementations.v1.cloudlets.Cloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.VoidCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class CloudletLifecycleTest
			extends BaseCloudletTest<BaseCloudletTest.BaseScenario<VoidCloudletContext>>
{
	@Override
	public void setUp () {
		this.scenario = new BaseScenario<VoidCloudletContext> ();
		BaseCloudletTest.setUpScenario (this.getClass (), this.scenario, null, Callback.class, VoidCloudletContext.class);
		this.cloudlet = Cloudlet.create (this.scenario.environment);
	}
	
	@Override
	public void test () {
		this.awaitSuccess (this.cloudlet.initialize ());
	}
	
	public static class Callback
				extends DefaultCloudletCallback<VoidCloudletContext>
	{
		public Callback (final CloudletController<VoidCloudletContext> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		protected CallbackCompletion<Void> destroy (final VoidCloudletContext context) {
			context.transcript.traceDebugging ("destroying...");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		protected CallbackCompletion<Void> initialize (final VoidCloudletContext context) {
			context.transcript.traceDebugging ("initializing...");
			return (DefaultCallback.Succeeded);
		}
	}
}
