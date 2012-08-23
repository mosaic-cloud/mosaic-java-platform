/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.cloudlets.tools;


import eu.mosaic_cloud.cloudlets.connectors.httpg.HttpgQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.generic.GenericKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache.MemcacheKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;

import com.google.common.base.Preconditions;


public class DefaultConnectorsFactory
		extends BaseConnectorsFactory
		implements
			eu.mosaic_cloud.cloudlets.connectors.core.IConnectorsFactory
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
		final eu.mosaic_cloud.connectors.tools.DefaultConnectorsFactory delegate = eu.mosaic_cloud.connectors.tools.DefaultConnectorsFactory.create (environment);
		return DefaultConnectorsFactory.create (cloudlet, environment, delegate);
	}
	
	public static final DefaultConnectorsFactory create (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		return DefaultConnectorsFactory.createBuilder (cloudlet, environment, delegate).build ();
	}
	
	public static final Builder createBuilder (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		final DefaultConnectorsFactory factory = new DefaultConnectorsFactory (cloudlet, environment, delegate);
		final Builder builder = factory.new Builder ();
		return (builder);
	}
	
	protected final ICloudletController<?> cloudlet;
	
	public final class Builder
			extends BaseConnectorsFactoryBuilder<DefaultConnectorsFactory>
	{
		Builder ()
		{
			super (DefaultConnectorsFactory.this);
			this.initialize ();
		}
		
		@Override
		protected final void initialize_1 ()
		{
			GenericKvStoreConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.cloudlet, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			MemcacheKvStoreConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.cloudlet, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			AmqpQueueConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.cloudlet, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			HttpgQueueConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.cloudlet, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
		}
	}
}
