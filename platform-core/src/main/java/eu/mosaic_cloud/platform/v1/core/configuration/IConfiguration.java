/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.platform.v1.core.configuration;


/**
 * Generic interface for handling configuration files.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 */
public interface IConfiguration
{
	/**
	 * Adds a parameter to the configuration.
	 * 
	 * @param identifier
	 *            the identifier of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	<T extends Object> void addParameter (final ConfigurationIdentifier identifier, final T value);
	
	/**
	 * Adds a parameter to the configuration.
	 * 
	 * @param property
	 *            the identifier of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	<T extends Object> void addParameter (final String property, final T value);
	
	/**
	 * Returns a configuration parameter.
	 * 
	 * @param <T>
	 *            the type of the parameter
	 * @param identifier
	 *            the identifier of the parameter
	 * @param valueClass
	 *            the class object representing the type of the configuration parameter
	 * @return the configuration parameter
	 */
	<T extends Object> IConfigurationParameter<T> getParameter (final ConfigurationIdentifier identifier, final Class<T> valueClass);
	
	/**
	 * Returns the root identifier.
	 * 
	 * @return the root identifier
	 */
	ConfigurationIdentifier getRootIdentifier ();
	
	/**
	 * Returns a configuration containing all parameters which names start with a given identifier (root identifier).
	 * 
	 * @param relative
	 *            the root identifier
	 * @return the configuration object
	 */
	IConfiguration spliceConfiguration (final ConfigurationIdentifier relative);
}
