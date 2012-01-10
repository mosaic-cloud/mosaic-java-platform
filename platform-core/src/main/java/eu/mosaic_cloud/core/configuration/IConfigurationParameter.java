/*
 * #%L
 * mosaic-core
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package eu.mosaic_cloud.core.configuration;

/**
 * Interface for configuration parameters.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * 
 * @param <T>
 *            the type of the value of the parameter
 */
public interface IConfigurationParameter<T extends Object> {

	/**
	 * Returns the configuration identifier of the parameter.
	 * 
	 * @return the configuration identifier of the parameter
	 */
	ConfigurationIdentifier getIdentifier();

	/**
	 * Returns the value of the parameter.
	 * 
	 * @param defaultValue
	 *            a default value of the parameter used in case the parameter is
	 *            not found in the configuration
	 * @return the value of the parameter
	 */
	T getValue(final T defaultValue);

	/**
	 * Returns the type of the value of the parameter.
	 * 
	 * @return the type of the value of the parameter
	 */
	Class<T> getValueClass();
}
