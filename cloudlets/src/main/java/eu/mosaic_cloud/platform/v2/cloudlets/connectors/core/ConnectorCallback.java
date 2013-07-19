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

package eu.mosaic_cloud.platform.v2.cloudlets.connectors.core;


import eu.mosaic_cloud.platform.v2.cloudlets.core.Callback;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface ConnectorCallback<TContext extends Object>
			extends
				Callback<TContext>
{
	public abstract CallbackCompletion<Void> destroyFailed (TContext context, DestroyFailedArguments arguments);
	
	public abstract CallbackCompletion<Void> destroySucceeded (TContext context, DestroySucceededArguments arguments);
	
	public abstract CallbackCompletion<Void> initializeFailed (TContext context, InitializeFailedArguments arguments);
	
	public abstract CallbackCompletion<Void> initializeSucceeded (TContext context, InitializeSucceededArguments arguments);
	
	public static final class DestroyFailedArguments
				extends ConnectorFailedArguments
	{
		public DestroyFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Throwable error) {
			super (cloudlet, connector, error);
		}
	}
	
	public static final class DestroySucceededArguments
				extends ConnectorSucceededArguments
	{
		public DestroySucceededArguments (final CloudletController<?> cloudlet, final Connector connector) {
			super (cloudlet, connector);
		}
	}
	
	public static final class InitializeFailedArguments
				extends ConnectorFailedArguments
	{
		public InitializeFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Throwable error) {
			super (cloudlet, connector, error);
		}
	}
	
	public static final class InitializeSucceededArguments
				extends ConnectorSucceededArguments
	{
		public InitializeSucceededArguments (final CloudletController<?> cloudlet, final Connector connector) {
			super (cloudlet, connector);
		}
	}
}
