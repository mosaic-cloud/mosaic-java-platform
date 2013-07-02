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


import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.executors.ExecutorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.httpg.HttpgQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.kvstore.generic.GenericKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.implementations.v1.connectors.queue.amqp.AmqpQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.ICloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.implementations.v1.core.BaseConnectorsFactory;
import eu.mosaic_cloud.connectors.implementations.v1.core.BaseConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactoryInitializer;

import com.google.common.base.Preconditions;


public class DefaultConnectorsFactory
		extends BaseConnectorsFactory
		implements
			eu.mosaic_cloud.cloudlets.v1.connectors.core.IConnectorsFactory
{
	protected DefaultConnectorsFactory (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		super (environment, delegate);
		Preconditions.checkNotNull (delegate);
		Preconditions.checkNotNull (cloudlet);
		this.cloudlet = cloudlet;
	}
	
	public static final DefaultConnectorsFactory create (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment)
	{
		final eu.mosaic_cloud.connectors.implementations.v1.core.DefaultConnectorsFactory delegate = eu.mosaic_cloud.connectors.implementations.v1.core.DefaultConnectorsFactory.create (environment);
		return DefaultConnectorsFactory.create (cloudlet, environment, delegate);
	}
	
	public static final DefaultConnectorsFactory create (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		return DefaultConnectorsFactory.Builder.create (cloudlet, environment, delegate).build ();
	}
	
	protected final ICloudletController<?> cloudlet;
	
	public static final class Builder
			extends BaseConnectorsFactoryBuilder<DefaultConnectorsFactory>
			implements
				IConnectorsFactoryBuilder
	{
		Builder (final DefaultConnectorsFactory factory, final ICloudletController<?> cloudlet)
		{
			super (factory);
			Preconditions.checkNotNull (this.delegate);
			Preconditions.checkNotNull (cloudlet);
			this.cloudlet = cloudlet;
			this.initialize ();
		}
		
		@Override
		public void initialize (final eu.mosaic_cloud.cloudlets.v1.connectors.core.IConnectorsFactoryInitializer initializer)
		{
			Preconditions.checkNotNull (initializer);
			initializer.initialize (this, this.cloudlet, this.environment, this.factory);
		}
		
		@Override
		public void initialize (final IConnectorsFactoryInitializer initializer)
		{
			Preconditions.checkNotNull (initializer);
			initializer.initialize (this, this.environment, this.factory);
		}
		
		@Override
		protected final void initialize_1 ()
		{
			this.initialize (GenericKvStoreConnectorFactoryInitializer.defaultInstance);
			this.initialize (AmqpQueueConnectorFactoryInitializer.defaultInstance);
			this.initialize (HttpgQueueConnectorFactoryInitializer.defaultInstance);
			this.initialize (ExecutorFactoryInitializer.defaultInstance);
		}
		
		public static final Builder create (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
		{
			final DefaultConnectorsFactory factory = new DefaultConnectorsFactory (cloudlet, environment, delegate);
			final Builder builder = new Builder (factory, cloudlet);
			return (builder);
		}
		
		protected final ICloudletController<?> cloudlet;
	}
}
