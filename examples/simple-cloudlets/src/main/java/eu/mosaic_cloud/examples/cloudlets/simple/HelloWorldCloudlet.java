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
package eu.mosaic_cloud.examples.cloudlets.simple;

import eu.mosaic_cloud.cloudlets.runtime.CloudletComponentLauncher;

import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;

public class HelloWorldCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<HelloCloudletContext> {

		@Override
		public void initialize(HelloCloudletContext context,
				CallbackArguments<HelloCloudletContext> arguments) {
			this.logger.info(
					"HelloWorld cloudlet is initializing...");
		}

		@Override
		public void initializeSucceeded(HelloCloudletContext context,
				CallbackArguments<HelloCloudletContext> arguments) {
			this.logger.info(
					"HelloWorld cloudlet was initialized successfully.");
			System.out.println("Hello world!");
			ICloudletController<HelloCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroy();
		}

		@Override
		public void destroy(HelloCloudletContext context,
				CallbackArguments<HelloCloudletContext> arguments) {
			this.logger.info(
					"HelloWorld cloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(HelloCloudletContext context,
				CallbackArguments<HelloCloudletContext> arguments) {
			this.logger.info(
					"HelloWorld cloudlet was destroyed successfully.");
		}

	}

	public static final class HelloCloudletContext {
	}

	public static void main(String[] arguments) throws Throwable {
		CloudletComponentLauncher.main("hello-cloudlet.prop", arguments);
	}
}
