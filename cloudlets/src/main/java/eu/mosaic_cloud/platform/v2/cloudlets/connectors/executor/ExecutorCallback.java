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

package eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor;


import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.Connector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface ExecutorCallback<TContext extends Object, TOutcome extends Object, TExtra extends Object>
			extends
				ConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> executionFailed (TContext context, ExecutionFailedArguments<TOutcome, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> executionSucceeded (TContext context, ExecutionSucceededArguments<TOutcome, TExtra> arguments);
	
	public static final class ExecutionFailedArguments<TOutcome extends Object, TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public ExecutionFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Callable<TOutcome> callable, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.callable = callable;
		}
		
		public final Callable<TOutcome> callable;
	}
	
	public static final class ExecutionSucceededArguments<TOutcome extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public ExecutionSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final Callable<TOutcome> callable, final TOutcome outcome, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.callable = callable;
			this.outcome = outcome;
		}
		
		public final Callable<TOutcome> callable;
		public final TOutcome outcome;
	}
}
