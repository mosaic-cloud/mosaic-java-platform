/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.implementations.v1.core;


import eu.mosaic_cloud.connectors.implementations.v1.httpg.HttpgQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.implementations.v1.kvstore.generic.GenericKvStoreConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.implementations.v1.queue.amqp.AmqpQueueConnectorFactoryInitializer;
import eu.mosaic_cloud.connectors.v1.core.ConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.v1.core.ZZZ_core_ConnectorsFactory;

import com.google.common.base.Preconditions;


public class DefaultConnectorsFactory
			extends BaseConnectorsFactory
{
	protected DefaultConnectorsFactory (final ConnectorEnvironment environment, final ZZZ_core_ConnectorsFactory delegate) {
		super (environment, delegate);
	}
	
	public static final DefaultConnectorsFactory create (final ConnectorEnvironment environment) {
		return DefaultConnectorsFactory.create (environment, null);
	}
	
	public static final DefaultConnectorsFactory create (final ConnectorEnvironment environment, final ZZZ_core_ConnectorsFactory delegate) {
		return DefaultConnectorsFactory.Builder.create (environment, delegate).build ();
	}
	
	public static final class Builder
				extends BaseConnectorsFactoryBuilder<DefaultConnectorsFactory>
	{
		Builder (final DefaultConnectorsFactory factory) {
			super (factory);
			this.initialize ();
		}
		
		@Override
		public void initialize (final ConnectorsFactoryInitializer initializer) {
			Preconditions.checkNotNull (initializer);
			initializer.initialize (this, this.environment, this.factory);
		}
		
		@Override
		protected final void initialize_1 () {
			this.initialize (GenericKvStoreConnectorFactoryInitializer.defaultInstance);
			this.initialize (AmqpQueueConnectorFactoryInitializer.defaultInstance);
			this.initialize (HttpgQueueConnectorFactoryInitializer.defaultInstance);
		}
		
		public static final Builder create (final ConnectorEnvironment environment, final ZZZ_core_ConnectorsFactory delegate) {
			final DefaultConnectorsFactory factory = new DefaultConnectorsFactory (environment, delegate);
			final Builder builder = new Builder (factory);
			return (builder);
		}
	}
}
