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

package eu.mosaic_cloud.platform.implementation.v2.connectors.core;


import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorVariant;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryInitializer;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactoryInitializer
			extends Object
			implements
				ConnectorsFactoryInitializer
{
	protected BaseConnectorsFactoryInitializer () {
		super ();
	}
	
	@Override
	public final void initialize (final ConnectorsFactoryBuilderInitializer builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		Preconditions.checkNotNull (builder);
		Preconditions.checkNotNull (environment);
		this.initialize_1 (builder, environment, delegate);
	}
	
	protected abstract void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate);
	
	protected final <TFactory extends ConnectorFactory<?>> void register (final ConnectorsFactoryBuilderInitializer builder, final Class<TFactory> factoryClass, final ConnectorVariant variant, final boolean enforceVariant, final boolean fallbackVariant, final TFactory factory) {
		Preconditions.checkNotNull (builder);
		Preconditions.checkNotNull (factoryClass);
		Preconditions.checkNotNull (factory);
		if (enforceVariant)
			Preconditions.checkNotNull (variant);
		if (variant != null)
			if (!builder.register (factoryClass, variant, factory))
				throw (new IllegalStateException ("variant is already registered"));
		if (fallbackVariant)
			builder.register (factoryClass, ConnectorVariant.fallback, factory);
	}
}
