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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.executor;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.implementation.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor.ExecutorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor.ExecutorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilder;


public final class ExecutorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilder builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		builder.register (ExecutorFactory.class, new ExecutorFactory () {
			@Override
			public <TContext, TOutcome, TExtra> Executor<TContext, TOutcome, TExtra> create (final Configuration configuration, final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
				return new Executor<TContext, TOutcome, TExtra> (cloudlet, environment.getThreading (), environment.getExceptions (), configuration, callback, callbackContext);
			}
		});
	}
	
	public static final ExecutorFactoryInitializer defaultInstance = new ExecutorFactoryInitializer ();
}
