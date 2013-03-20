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

package eu.mosaic_cloud.cloudlets.connectors.executors;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.UncaughtExceptionHandler;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFutureTask;


public class Executor<TContext, TOutcome, TExtra>
		implements
			IExecutor<TOutcome, TExtra>,
			CallbackProxy
{
	public Executor (final ICloudletController<?> cloudlet, final ThreadingContext threading, final ExceptionTracer exceptions, final IConfiguration configuration, final IExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext context)
	{
		super ();
		Preconditions.checkNotNull (cloudlet);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		Preconditions.checkNotNull (configuration);
		this.cloudlet = cloudlet;
		this.configuration = configuration;
		this.callback = callback;
		this.context = context;
		final ThreadConfiguration executorConfiguration = threading.getThreadConfiguration ().override (this, "tasks", true, exceptions, UncaughtExceptionHandler.create (exceptions));
		this.executor = threading.createFixedThreadPool (executorConfiguration, 1);
		this.transcript = Transcript.create (this, true);
		this.transcript.traceDebugging ("creating the cloudlet executor...");
		this.transcript.traceDebugging ("used by the cloudlet `%{object}`...", this.cloudlet);
		this.transcript.traceDebugging ("using the completion callbacks `%{object}`...", this.callback);
		this.transcript.traceDebugging ("using the backing threading `%{}`...", threading);
		this.transcript.traceDebugging ("using the backing executor `%{}`...", this.executor);
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		// FIXME: We should return success only on actual destruction...
		this.executor.shutdownNow ();
		return (CallbackCompletion.createOutcome ());
	}
	
	@Override
	public CallbackCompletion<TOutcome> execute (final Callable<TOutcome> callable, final TExtra extra)
	{
		final ListenableFutureTask<TOutcome> task = ListenableFutureTask.create (callable);
		this.executor.execute (task);
		final CallbackCompletion<TOutcome> completion = CallbackCompletion.createDeferred (task);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					CallbackCompletion<Void> result;
					if (completion.getException () == null) {
						result = Executor.this.callback.executionSucceeded (Executor.this.context, new ExecutionSucceededCallbackArguments<TOutcome, TExtra> (Executor.this.cloudlet, completion.getOutcome (), extra));
					} else {
						result = Executor.this.callback.executionFailed (Executor.this.context, new ExecutionFailedCallbackArguments<TExtra> (Executor.this.cloudlet, completion.getException (), extra));
					}
					return result;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Void> initialize ()
	{
		return (CallbackCompletion.createOutcome ());
	}
	
	protected final IExecutorCallback<TContext, TOutcome, TExtra> callback;
	protected final ICloudletController<?> cloudlet;
	protected final IConfiguration configuration;
	protected final TContext context;
	protected final ExecutorService executor;
	protected final Transcript transcript;
}
