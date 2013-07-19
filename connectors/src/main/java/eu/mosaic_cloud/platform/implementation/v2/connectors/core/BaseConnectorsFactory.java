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


import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactory
			extends Object
			implements
				ConnectorsFactory
{
	protected BaseConnectorsFactory (final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		super ();
		Preconditions.checkNotNull (environment);
		this.monitor = Monitor.create (this);
		this.environment = environment;
		this.delegate = delegate;
		this.factories = new ConcurrentHashMap<Class<? extends ConnectorFactory<?>>, ConnectorFactory<?>> ();
	}
	
	@Override
	public <Factory extends ConnectorFactory<?>> Factory getConnectorFactory (final Class<Factory> factoryClass) {
		Factory factory = factoryClass.cast (this.factories.get (factoryClass));
		if ((factory == null) && (this.delegate != null)) {
			factory = this.delegate.getConnectorFactory (factoryClass);
		}
		return factory;
	}
	
	protected final <Factory extends ConnectorFactory<?>> void registerFactory (final Class<Factory> factoryClass, final Factory factory) {
		Preconditions.checkNotNull (factoryClass);
		Preconditions.checkArgument (factoryClass.isInterface ());
		Preconditions.checkArgument (ConnectorFactory.class.isAssignableFrom (factoryClass));
		Preconditions.checkNotNull (factory);
		Preconditions.checkArgument (factoryClass.isInstance (factory));
		synchronized (this.monitor) {
			Preconditions.checkState (!this.factories.containsKey (factoryClass));
			this.factories.put (factoryClass, factory);
		}
	}
	
	protected final ConnectorsFactory delegate;
	protected final ConnectorEnvironment environment;
	protected final ConcurrentHashMap<Class<? extends ConnectorFactory<?>>, ConnectorFactory<?>> factories;
	protected final Monitor monitor;
}
