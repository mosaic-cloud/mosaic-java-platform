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

package eu.mosaic_cloud.cloudlets.v1.cloudlets;


import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface CloudletCallback<TContext extends Object>
			extends
				Callback<TContext>
{
	public abstract CallbackCompletion<Void> destroy (TContext context, DestroyArguments arguments);
	
	public abstract CallbackCompletion<Void> destroyFailed (TContext context, DestroyFailedArguments arguments);
	
	public abstract CallbackCompletion<Void> destroySucceeded (TContext context, DestroySucceededArguments arguments);
	
	public abstract CallbackCompletion<Void> initialize (TContext context, InitializeArguments arguments);
	
	public abstract CallbackCompletion<Void> initializeFailed (TContext context, InitializeFailedArguments arguments);
	
	public abstract CallbackCompletion<Void> initializeSucceeded (TContext context, InitializeSucceededArguments arguments);
	
	public static final class DestroyArguments
				extends CloudletCallbackArguments
	{
		public DestroyArguments (final CloudletController<?> cloudlet) {
			super (cloudlet);
		}
	}
	
	public static final class DestroyFailedArguments
				extends CloudletFailedArguments
	{
		public DestroyFailedArguments (final CloudletController<?> cloudlet, final Throwable error) {
			super (cloudlet, error);
		}
	}
	
	public static final class DestroySucceededArguments
				extends CloudletSucceededArguments
	{
		public DestroySucceededArguments (final CloudletController<?> cloudlet) {
			super (cloudlet);
		}
	}
	
	public static final class InitializeArguments
				extends CloudletCallbackArguments
	{
		public InitializeArguments (final CloudletController<?> cloudlet) {
			super (cloudlet);
		}
	}
	
	public static final class InitializeFailedArguments
				extends CloudletFailedArguments
	{
		public InitializeFailedArguments (final CloudletController<?> cloudlet, final Throwable error) {
			super (cloudlet, error);
		}
	}
	
	public static final class InitializeSucceededArguments
				extends CloudletSucceededArguments
	{
		public InitializeSucceededArguments (final CloudletController<?> cloudlet) {
			super (cloudlet);
		}
	}
}
