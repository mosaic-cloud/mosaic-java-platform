/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple;


import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class HelloWorldCloudlet
			extends DefaultCloudlet
{
	public static class Callback
				extends DefaultCloudletCallback<Context>
	{
		public Callback (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final CloudletCallbackArguments<Context> arguments) {
			context.logger.info ("HelloWorldCloudlet destroying...");
			return (eu.mosaic_cloud.cloudlets.v1.core.Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments) {
			context.logger.info ("HelloWorldCloudlet destroyed successfully.");
			return (eu.mosaic_cloud.cloudlets.v1.core.Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final CloudletCallbackArguments<Context> arguments) {
			context.logger.info ("HelloWorldCloudlet initializing...");
			return (eu.mosaic_cloud.cloudlets.v1.core.Callback.SUCCESS);
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments) {
			context.logger.info ("HelloWorldCloudlet initialized successfully.");
			context.logger.info ("HelloWorldCloudlet greats the cloud developer!");
			context.cloudlet.destroy ();
			return (eu.mosaic_cloud.cloudlets.v1.core.Callback.SUCCESS);
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
	}
}
