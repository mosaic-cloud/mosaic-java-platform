/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.platform.core.configuration;


/**
 * Utility functions used for handling configurations.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ConfigUtils
{
	private ConfigUtils ()
	{}
	
	/**
	 * Resolves a configuration parameter given its relative specification.
	 * 
	 * @param <T>
	 *            the type of the parameter
	 * @param configuration
	 *            the configuration to which the parameter belongs
	 * @param identifier
	 *            the relative identifier of the parameter
	 * @param valueClass
	 *            the class object representing the type of the parameter
	 * @param defaultValue
	 *            a default value to give to the parameter if it is not found in
	 *            the given configuration
	 * @return the value of the parameter
	 */
	public static <T extends Object> T resolveParameter (final IConfiguration configuration, final String identifier, final Class<T> valueClass, final T defaultValue) // NOPMD
	{ // NOPMD
		T retValue;
		if (configuration == null) {
			retValue = defaultValue;
		} else {
			if (valueClass.isEnum ()) {
				final String retName = configuration.getParameter (ConfigurationIdentifier.resolveRelative (identifier), String.class).getValue (((Enum<?>) defaultValue).name ());
				retValue = (T) Enum.valueOf ((Class) valueClass, retName.toUpperCase ());
			} else {
				retValue = configuration.getParameter (ConfigurationIdentifier.resolveRelative (identifier), valueClass).getValue (defaultValue);
			}
		}
		return retValue;
	}
}
