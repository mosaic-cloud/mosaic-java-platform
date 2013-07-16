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

package eu.mosaic_cloud.cloudlets.implementations.v1.connectors.core;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback.DestroyFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback.DestroySucceededArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback.InitializeFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback.InitializeSucceededArguments;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;


/**
 * Implements the life-cycle operations required by the cloudlet-level connector. All cloudlet-level connnectors should extend
 * this class.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * @param <TConnector>
 *            lower level resource-specific connector
 * @param <TCallback>
 *            resource life-cycle callback class defined by the developer of the cloudlet
 * @param <TContext>
 *            cloudlet callback context
 */
public abstract class BaseConnector<TConnector extends eu.mosaic_cloud.connectors.v1.core.Connector, TCallback extends ConnectorCallback<TContext>, TContext extends Object>
			implements
				Connector,
				CallbackProxy
{
	protected BaseConnector (final CloudletController<?> cloudlet, final TConnector connector, final Configuration configuration, final TCallback callback, final TContext context) {
		super ();
		Preconditions.checkNotNull (cloudlet);
		Preconditions.checkNotNull (connector);
		Preconditions.checkNotNull (configuration);
		this.cloudlet = cloudlet;
		this.connector = connector;
		this.configuration = configuration;
		this.callback = callback;
		this.context = context;
		this.transcript = Transcript.create (this, true);
		this.transcript.traceDebugging ("creating the cloudlet connector adapter of type `%{object:class}`...", this);
		this.transcript.traceDebugging ("used by the cloudlet `%{object}`...", this.cloudlet);
		this.transcript.traceDebugging ("using the underlying connector `%{object}`...", this.connector);
		this.transcript.traceDebugging ("using the completion callbacks `%{object}`...", this.callback);
	}
	
	@Override
	public CallbackCompletion<Void> destroy () {
		return this.destroy (true);
	}
	
	@Override
	public CallbackCompletion<Void> initialize () {
		return this.initialize (true);
	}
	
	protected CallbackCompletion<Void> destroy (final boolean propagate) {
		this.transcript.traceDebugging ("destroying the connector adapter...");
		final CallbackCompletion<Void> completion;
		if (propagate) {
			completion = this.connector.destroy ();
		} else {
			completion = CallbackCompletion.createOutcome ();
		}
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
					assert (completion_ == completion);
					if (completion.getException () != null) {
						BaseConnector.this.transcript.traceDebugging ("triggering the callback for destroy failure...");
						return BaseConnector.this.callback.destroyFailed (BaseConnector.this.context, new DestroyFailedArguments (BaseConnector.this.cloudlet, BaseConnector.this, completion.getException ()));
					}
					BaseConnector.this.transcript.traceDebugging ("triggering the callback for destroy success...");
					return BaseConnector.this.callback.destroySucceeded (BaseConnector.this.context, new DestroySucceededArguments (BaseConnector.this.cloudlet, BaseConnector.this));
				}
			});
		}
		return completion;
	}
	
	protected CallbackCompletion<Void> initialize (final boolean propagate) {
		this.transcript.traceDebugging ("initializing the connector adapter...");
		final CallbackCompletion<Void> completion;
		if (propagate) {
			completion = this.connector.initialize ();
		} else {
			completion = CallbackCompletion.createOutcome ();
		}
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
					assert (completion_ == completion);
					if (completion.getException () != null) {
						BaseConnector.this.transcript.traceDebugging ("triggering the callback for initialize failure...");
						return BaseConnector.this.callback.initializeFailed (BaseConnector.this.context, new InitializeFailedArguments (BaseConnector.this.cloudlet, BaseConnector.this, completion.getException ()));
					}
					BaseConnector.this.transcript.traceDebugging ("triggering the callback for initialize success...");
					return BaseConnector.this.callback.initializeSucceeded (BaseConnector.this.context, new InitializeSucceededArguments (BaseConnector.this.cloudlet, BaseConnector.this));
				}
			});
		}
		return completion;
	}
	
	protected final TCallback callback;
	protected final CloudletController<?> cloudlet;
	protected final Configuration configuration;
	protected final TConnector connector;
	protected final TContext context;
	protected final Transcript transcript;
}
