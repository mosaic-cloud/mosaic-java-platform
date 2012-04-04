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

import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class HelloWorldCloudlet {

    public static final class HelloCloudletContext {

        ICloudletController<HelloCloudletContext> cloudlet;
    }

    public static final class LifeCycleHandler extends
            DefaultCloudletCallback<HelloCloudletContext> {

        @Override
        public CallbackCompletion<Void> destroy(HelloCloudletContext context,
                CloudletCallbackArguments<HelloCloudletContext> arguments) {
            this.logger.info("HelloWorld cloudlet is being destroyed.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(HelloCloudletContext context,
                CloudletCallbackCompletionArguments<HelloCloudletContext> arguments) {
            this.logger.info("HelloWorld cloudlet was destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initialize(HelloCloudletContext context,
                CloudletCallbackArguments<HelloCloudletContext> arguments) {
            this.logger.info("HelloWorld cloudlet is initializing...");
            context.cloudlet = arguments.getCloudlet();
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(HelloCloudletContext context,
                CloudletCallbackCompletionArguments<HelloCloudletContext> arguments) {
            this.logger.info("HelloWorld cloudlet was initialized successfully.");
            this.logger.info("Hello world!");
            context.cloudlet.destroy();
            return ICallback.SUCCESS;
        }
    }
}
