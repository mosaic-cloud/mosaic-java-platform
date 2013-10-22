/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.platform.implementation.v2.connectors.core;


import eu.mosaic_cloud.platform.v2.connectors.core.Connector;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionTrigger;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;


public abstract class BaseConnector<TProxy extends BaseConnectorProxy>
			implements
				Connector,
				CallbackProxy
{
	protected BaseConnector (final TProxy proxy, final ConnectorConfiguration configuration) {
		super ();
		Preconditions.checkNotNull (proxy);
		Preconditions.checkNotNull (configuration);
		this.proxy = proxy;
		this.configuration = configuration;
		this.transcript = Transcript.create (this, true);
		this.transcript.traceDebugging ("creating the connector of type `%{object:class}`.", this);
		this.transcript.traceDebugging ("using the underlying connector proxy `%{object}`...", this.proxy);
		this.isolate = this.configuration.getEnvironment ().getReactor ().createIsolate ();
	}
	
	@Override
	public final CallbackCompletion<Void> destroy () {
		return (this.enqueueOperation (new Operation<Void> () {
			@Override
			public final CallbackCompletion<Void> execute (final CallbackCompletionTrigger<Void> trigger) {
				return (BaseConnector.this.proxy.destroy (trigger));
			}
		}));
	}
	
	@Override
	public final CallbackCompletion<Void> initialize () {
		return (this.enqueueOperation (new Operation<Void> () {
			@Override
			public final CallbackCompletion<Void> execute (final CallbackCompletionTrigger<Void> trigger) {
				return (BaseConnector.this.proxy.initialize (trigger));
			}
		}));
	}
	
	protected final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> enqueueOperation (final Operation<_Outcome_> operation) {
		Preconditions.checkNotNull (operation);
		final CallbackCompletionTrigger<_Outcome_> trigger = CallbackCompletionTrigger.create ();
		this.isolate.enqueue (new Runnable () {
			@Override
			public final void run () {
				final CallbackCompletion<_Outcome_> completion = operation.execute (trigger);
				Preconditions.checkState (completion == trigger.completion);
			}
		});
		return (trigger.completion);
	}
	
	protected final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> enqueueOperation (final Operation<_Outcome_> operation, final CallbackCompletionObserver observer) {
		Preconditions.checkNotNull (operation);
		Preconditions.checkNotNull (observer);
		final CallbackCompletion<_Outcome_> completion = this.enqueueOperation (operation);
		completion.observe (new CallbackCompletionObserver () {
			@Override
			public final CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
				Preconditions.checkArgument (completion == completion_);
				return (BaseConnector.this.isolate.enqueue (new Runnable () {
					@Override
					public final void run () {
						observer.completed (completion);
					}
				}));
			}
		});
		return (completion);
	}
	
	protected final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> enqueueOperation (final Operation<_Outcome_> operation, final OperationGuard<_Outcome_> guard) {
		Preconditions.checkNotNull (operation);
		Preconditions.checkNotNull (guard);
		final CallbackCompletion<_Outcome_> executeCompletion = this.enqueueOperation (operation);
		final CallbackCompletionTrigger<_Outcome_> enforceTrigger = CallbackCompletionTrigger.create ();
		executeCompletion.observe (new CallbackCompletionObserver () {
			@Override
			public final CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
				Preconditions.checkArgument (executeCompletion == completion_);
				return (BaseConnector.this.isolate.enqueue (new Runnable () {
					@Override
					public final void run () {
						final CallbackCompletion<_Outcome_> enforceCompletion = guard.enforce (executeCompletion, enforceTrigger);
						Preconditions.checkState (enforceCompletion == enforceTrigger.completion);
					}
				}));
			}
		});
		return (enforceTrigger.completion);
	}
	
	protected final ConnectorConfiguration configuration;
	protected final CallbackIsolate isolate;
	protected final TProxy proxy;
	protected final Transcript transcript;
	
	protected static interface Operation<_Outcome_ extends Object>
	{
		public abstract CallbackCompletion<_Outcome_> execute (final CallbackCompletionTrigger<_Outcome_> trigger);
	}
	
	protected static interface OperationGuard<_Outcome_ extends Object>
	{
		public abstract CallbackCompletion<_Outcome_> enforce (final CallbackCompletion<_Outcome_> completion, final CallbackCompletionTrigger<_Outcome_> trigger);
	}
}
