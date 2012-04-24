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


import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;

import com.google.common.base.Preconditions;


public final class ConnectorConfiguration
{
	private ConnectorConfiguration (final IConfiguration configuration, final ConnectorEnvironment environment)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (environment);
		this.configuration = configuration;
		this.environment = environment;
	}
	
	public Channel getCommunicationChannel ()
	{
		return this.environment.getCommunicationChannel ();
	}
	
	public <T extends Object> T getConfigParameter (final String identifier, final Class<T> valueClass, final T defaultValue)
	{
		return ConfigUtils.resolveParameter (this.configuration, identifier, valueClass, defaultValue);
	}
	
	public void resolveChannel (final String driverTarget, final ResolverCallbacks resolverCallbacks)
	{
		this.environment.resolveChannel (driverTarget, resolverCallbacks);
	}
	
	public static ConnectorConfiguration create (final IConfiguration configuration, final ConnectorEnvironment environment)
	{
		return new ConnectorConfiguration (configuration, environment);
	}
	
	/**
	 * Configuration settings private to a single connector.
	 */
	private final IConfiguration configuration;
	/**
	 * Configuration settings which can be applied to one or more connectors
	 * (shared).
	 */
	private final ConnectorEnvironment environment;
}
