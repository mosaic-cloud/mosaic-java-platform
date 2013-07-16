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

package eu.mosaic_cloud.cloudlets.v1.connectors.components;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface ComponentConnectorCallbacks<TContext extends Object, TExtra extends Object>
			extends
				ConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> acquireFailed (TContext context, AcquireFailedArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> acquireSucceeded (TContext context, AcquireSucceededArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> callFailed (TContext context, CallFailedArguments<?, ?, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> callSucceeded (TContext context, CallSucceededArguments<?, ?, TExtra> arguments);
	
	public static final class AcquireFailedArguments<TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public AcquireFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final ComponentResourceSpecification resource, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.resource = resource;
		}
		
		public final ComponentResourceSpecification resource;
	}
	
	public static final class AcquireSucceededArguments<TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public AcquireSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final ComponentResourceSpecification resource, final ComponentResourceDescriptor descriptor, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.resource = resource;
			this.descriptor = descriptor;
		}
		
		public final ComponentResourceDescriptor descriptor;
		public final ComponentResourceSpecification resource;
	}
	
	public static final class CallFailedArguments<TInputs extends Object, TOutputs extends Object, TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public CallFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputsExpected, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.component = component;
			this.operation = operation;
			this.inputs = inputs;
			this.outputsExpected = outputsExpected;
		}
		
		public final ComponentIdentifier component;
		public final TInputs inputs;
		public final String operation;
		public final Class<TOutputs> outputsExpected;
	}
	
	public static final class CallSucceededArguments<TInputs extends Object, TOutputs extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public CallSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputsExpected, final TOutputs outputs, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.component = component;
			this.operation = operation;
			this.inputs = inputs;
			this.outputsExpected = outputsExpected;
			this.outputs = outputs;
		}
		
		public final ComponentIdentifier component;
		public final TInputs inputs;
		public final String operation;
		public final TOutputs outputs;
		public final Class<TOutputs> outputsExpected;
	}
}
