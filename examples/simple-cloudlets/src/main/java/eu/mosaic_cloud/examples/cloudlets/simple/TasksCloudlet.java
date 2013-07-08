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


import java.util.UUID;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultContext;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultExecutorCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.ExecutionSucceededCallbackArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.Executor;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.ExecutorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class TasksCloudlet
{
	public static final class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		@Override
		public CallbackCompletion<Void> destroy (final Context context, final CloudletCallbackArguments<Context> arguments) {
			context.logger.info ("destroying cloudlet...");
			context.logger.info ("destroying executor...");
			return context.executor.destroy ();
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments) {
			context.logger.info ("cloudlet destroyed successfully.");
			return Callback.SUCCESS;
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final Context context, final CloudletCallbackArguments<Context> arguments) {
			context.logger.info ("initializing cloudlet...");
			context.cloudletCallback = this;
			context.executorCallback = new ExecutorCallback ();
			context.workflow = new Workflow ();
			context.cloudlet = arguments.getCloudlet ();
			final Configuration configuration = context.cloudlet.getConfiguration ();
			context.logger.info ("creating executor...");
			final Configuration executorConfiguration = configuration.spliceConfiguration (ConfigurationIdentifier.resolveAbsolute ("executor"));
			context.executor = context.cloudlet.getConnectorFactory (ExecutorFactory.class).create (executorConfiguration, context.executorCallback, context);
			context.logger.info ("initializing queue connectors...");
			return context.executor.initialize ();
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments) {
			context.logger.info ("cloudlet initialized successfully.");
			return context.workflow.submitExecution (context);
		}
	}
	
	public static class Context
				extends DefaultContext
	{
		CloudletController<Context> cloudlet;
		CloudletCallback cloudletCallback;
		int counter;
		Executor<String, String> executor;
		ExecutorCallback executorCallback;
		Workflow workflow;
	}
	
	static class ExecutorCallback
				extends DefaultExecutorCallback<Context, String, String>
	{
		@Override
		public CallbackCompletion<Void> executionSucceeded (final Context context, final ExecutionSucceededCallbackArguments<String, String> arguments) {
			return context.workflow.handleOutcome (context, arguments.getOutcome (), arguments.getExtra ());
		}
	}
	
	static class TimeConsumingOperation
				implements
					Callable<String>
	{
		TimeConsumingOperation (final Context context, final String input) {
			super ();
			this.context = context;
			this.input = input;
		}
		
		@Override
		public String call () {
			this.context.logger.info ("executing time-consuming operation...");
			return new StringBuilder ().append (this.input).reverse ().toString ();
		}
		
		Context context;
		String input;
	}
	
	static class Workflow
	{
		CallbackCompletion<Void> handleOutcome (final Context context, final String output, final String input) {
			context.logger.info ("succeeded task with input `{}` and output `{}`...", input, output);
			context.counter++;
			if (context.counter == 10) {
				context.cloudlet.destroy ();
				return Callback.SUCCESS;
			}
			return (context.workflow.submitExecution (context));
		}
		
		CallbackCompletion<Void> submitExecution (final Context context) {
			final String input = UUID.randomUUID ().toString ();
			context.logger.info ("scheduling task with input `{}`...", input);
			context.executor.execute (new TimeConsumingOperation (context, input), input);
			return Callback.SUCCESS;
		}
	}
}
