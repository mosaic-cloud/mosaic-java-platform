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

package eu.mosaic_cloud.cloudlets.implementations.v1.connectors.executors;


import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.ICloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.IExecutor;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.IExecutorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.IExecutorFactory;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;


public final class ExecutorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IExecutorFactory.class, new IExecutorFactory () {
			@Override
			public <TContext, TOutcome, TExtra> IExecutor<TOutcome, TExtra> create (final IConfiguration configuration, final IExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext)
			{
				return new Executor<TContext, TOutcome, TExtra> (cloudlet, environment.getThreading (), environment.getExceptions (), configuration, callback, callbackContext);
			}
		});
	}
	
	public static final ExecutorFactoryInitializer defaultInstance = new ExecutorFactoryInitializer ();
}
