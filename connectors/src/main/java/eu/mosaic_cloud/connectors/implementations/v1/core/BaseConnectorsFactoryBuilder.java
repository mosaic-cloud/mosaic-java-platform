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


import java.util.concurrent.atomic.AtomicBoolean;

import eu.mosaic_cloud.connectors.v1.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.IConnectorsFactoryBuilder;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactoryBuilder<TFactory extends BaseConnectorsFactory>
			extends Object
			implements
				IConnectorsFactoryBuilder
{
	protected BaseConnectorsFactoryBuilder (final TFactory factory) {
		super ();
		Preconditions.checkNotNull (factory);
		this.factory = factory;
		this.environment = this.factory.environment;
		this.delegate = this.factory.delegate;
		this.built = new AtomicBoolean (false);
	}
	
	@Override
	public final TFactory build () {
		Preconditions.checkState (this.built.compareAndSet (false, true));
		this.build_1 ();
		return (this.factory);
	}
	
	@Override
	public final <TConnectorFactory extends IConnectorFactory<?>> void register (final Class<TConnectorFactory> factoryClass, final TConnectorFactory factory) {
		Preconditions.checkState (!this.built.get ());
		Preconditions.checkNotNull (factoryClass);
		Preconditions.checkArgument (factoryClass.isInterface ());
		Preconditions.checkArgument (IConnectorFactory.class.isAssignableFrom (factoryClass));
		Preconditions.checkNotNull (factory);
		Preconditions.checkArgument (factoryClass.isInstance (factory));
		this.factory.registerFactory (factoryClass, factory);
	}
	
	protected void build_1 () {}
	
	protected void initialize () {
		this.initialize_1 ();
	}
	
	protected void initialize_1 () {}
	
	protected <TConnectorFactory extends IConnectorFactory<?>> void register_1 (final Class<TConnectorFactory> factoryClass, final TConnectorFactory factory) {
		this.factory.registerFactory (factoryClass, factory);
	}
	
	protected final IConnectorsFactory delegate;
	protected final ConnectorEnvironment environment;
	protected final TFactory factory;
	private final AtomicBoolean built;
}
