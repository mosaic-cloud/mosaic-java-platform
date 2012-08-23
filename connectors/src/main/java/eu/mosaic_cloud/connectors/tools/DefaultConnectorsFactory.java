/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.tools;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.httpg.HttpgQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConnectorFactoryInitializer;


public class DefaultConnectorsFactory
		extends BaseConnectorsFactory
{
	protected DefaultConnectorsFactory (final IConnectorsFactory delegate, final ConnectorEnvironment environment)
	{
		super (delegate, environment);
	}
	
	public static final DefaultConnectorsFactory create (final ConnectorEnvironment environment)
	{
		return DefaultConnectorsFactory.createBuilder (null, environment).build ();
	}
	
	public static final DefaultConnectorsFactory create (final IConnectorsFactory delegate, final ConnectorEnvironment environment)
	{
		return DefaultConnectorsFactory.createBuilder (delegate, environment).build ();
	}
	
	public static final Builder createBuilder (final IConnectorsFactory delegate, final ConnectorEnvironment environment)
	{
		final DefaultConnectorsFactory factory = new DefaultConnectorsFactory (delegate, environment);
		final Builder builder = factory.new Builder ();
		return (builder);
	}
	
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
			GenericKvStoreConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			MemcacheKvStoreConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			AmqpQueueConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
			HttpgQueueConnectorFactoryInitializer.defaultInstance.initialize (this, DefaultConnectorsFactory.this.environment, DefaultConnectorsFactory.this.delegate);
		}
	}
}
