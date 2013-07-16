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

package eu.mosaic_cloud.cloudlets.v1.connectors.kvstore;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationFailedArguments;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorOperationSucceededArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface KvStoreConnectorCallback<TContext extends Object, TValue extends Object, TExtra extends Object>
			extends
				ConnectorCallback<TContext>
{
	public abstract CallbackCompletion<Void> deleteFailed (TContext context, DeleteFailedArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> deleteSucceeded (TContext context, DeleteSucceededArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> getFailed (TContext context, GetFailedArguments<TExtra> arguments);
	
	public abstract CallbackCompletion<Void> getSucceeded (TContext context, GetSucceededArguments<TValue, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> setFailed (TContext context, SetFailedArguments<TValue, TExtra> arguments);
	
	public abstract CallbackCompletion<Void> setSucceeded (TContext context, SetSucceededArguments<TValue, TExtra> arguments);
	
	public static final class DeleteFailedArguments<TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public DeleteFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.key = key;
		}
		
		public final String key;
	}
	
	public static final class DeleteSucceededArguments<TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public DeleteSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.key = key;
		}
		
		public final String key;
	}
	
	public static final class GetFailedArguments<TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public GetFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.key = key;
		}
		
		public final String key;
	}
	
	public static final class GetSucceededArguments<TValue extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public GetSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final TValue value, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.key = key;
			this.value = value;
		}
		
		public final String key;
		public final TValue value;
	}
	
	public static final class SetFailedArguments<TValue, TExtra extends Object>
				extends ConnectorOperationFailedArguments<TExtra>
	{
		public SetFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final TValue value, final Throwable error, final TExtra extra) {
			super (cloudlet, connector, error, extra);
			this.key = key;
			this.value = value;
		}
		
		public final String key;
		public final TValue value;
	}
	
	public static final class SetSucceededArguments<TValue extends Object, TExtra extends Object>
				extends ConnectorOperationSucceededArguments<TExtra>
	{
		public SetSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final String key, final TValue value, final TExtra extra) {
			super (cloudlet, connector, extra);
			this.key = key;
			this.value = value;
		}
		
		public final String key;
		public final TValue value;
	}
}
