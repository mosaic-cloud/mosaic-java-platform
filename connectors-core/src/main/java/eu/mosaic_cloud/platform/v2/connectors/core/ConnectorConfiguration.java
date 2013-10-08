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

package eu.mosaic_cloud.platform.v2.connectors.core;


import eu.mosaic_cloud.platform.implementation.v2.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;

import com.google.common.base.Preconditions;


public final class ConnectorConfiguration
{
	private ConnectorConfiguration (final Configuration configuration, final ConnectorEnvironment environment) {
		super ();
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (environment);
		this.configuration = configuration;
		this.environment = environment;
	}
	
	public <T extends Object> T getConfigParameter (final String identifier, final Class<T> valueClass, final T defaultValue) {
		return ConfigUtils.resolveParameter (this.configuration, identifier, valueClass, defaultValue);
	}
	
	public Configuration getConfiguration () {
		return this.configuration;
	}
	
	public ConnectorEnvironment getEnvironment () {
		return this.environment;
	}
	
	/**
	 * Configuration settings private to a single connector.
	 */
	private final Configuration configuration;
	/**
	 * Configuration settings which can be applied to one or more connectors (shared).
	 */
	private final ConnectorEnvironment environment;
	
	public static ConnectorConfiguration create (final Configuration configuration, final ConnectorEnvironment environment) {
		return new ConnectorConfiguration (configuration, environment);
	}
}
