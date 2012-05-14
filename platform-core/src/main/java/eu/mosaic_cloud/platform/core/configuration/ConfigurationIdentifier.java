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


import java.util.IdentityHashMap;
import java.util.regex.Pattern;


/**
 * Class for handling identifiers of configuration parameters. The parameters in
 * a configuration file may form a hierarchy and thus an identifier string may
 * be absolute or relative.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * 
 */
public final class ConfigurationIdentifier
{
	private ConfigurationIdentifier (final boolean absolute, final String identifier)
	{
		super ();
		this.absolute = absolute;
		this.identifier = identifier;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (final Object identifier)
	{
		boolean isEqual;
		if (identifier == null) {
			isEqual = false;
		} else if (identifier instanceof ConfigurationIdentifier) {
			final ConfigurationIdentifier otherId = (ConfigurationIdentifier) identifier;
			isEqual = (this.absolute == otherId.absolute) && (((this.identifier != null) && this.identifier.equals (otherId.identifier)) || ((this.identifier == null) && (otherId.identifier == null)));
		} else {
			isEqual = false;
		}
		return isEqual;
	}
	
	/**
	 * Returns the string identifier.
	 * 
	 * @return the string identifier
	 */
	public String getIdentifier ()
	{
		return this.identifier;
	}
	
	@Override
	public int hashCode ()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (this.absolute ? 1231 : 1237);
		result = (prime * result) + ((this.identifier == null) ? 0 : this.identifier.hashCode ());
		return result;
	}
	
	/**
	 * Returns if this identifier is absolute or not.
	 * 
	 * @return <code>true</code> if it is absolute
	 */
	public boolean isAbsolute ()
	{
		return this.absolute;
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter relative to this configuration identifier.
	 * 
	 * @param identifier
	 *            the identifier to solve
	 * @return the built identifier
	 */
	public ConfigurationIdentifier resolve (final ConfigurationIdentifier identifier)
	{
		if (identifier.absolute) {
			throw new IllegalArgumentException ();
		}
		return ConfigurationIdentifier.resolveRelative (this, identifier.identifier);
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter relative to this configuration identifier.
	 * 
	 * @param specification
	 *            the string specification of the identifier to be built
	 * @return the built identifier
	 */
	public ConfigurationIdentifier resolve (final String specification)
	{
		return ConfigurationIdentifier.resolve (this, specification);
	}
	
	@Override
	public String toString ()
	{
		return this.identifier;
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter given by an absolute identifier.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveAbsolute (final String specification)
	{
		return ConfigurationIdentifier.resolve (ConfigurationIdentifier.ROOT, specification);
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveRelative (final ConfigurationIdentifier reference, final String specification)
	{
		return ConfigurationIdentifier.resolve (reference, specification);
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveRelative (final String specification)
	{
		return ConfigurationIdentifier.resolve (null, specification);
	}
	
	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	private static ConfigurationIdentifier resolve (final ConfigurationIdentifier reference, final String specification)
	{
		boolean isAbsolute = false;
		String identifier_;
		String identifier;
		ConfigurationIdentifier parameterIdentifier;
		if (reference == null) {
			if (specification.charAt (0) == '/') {
				isAbsolute = true;
			}
			identifier_ = specification;
		} else if (reference.absolute) {
			isAbsolute = true;
			if (reference.identifier == null) {
				identifier_ = "/" + specification;
			} else {
				identifier_ = reference.identifier + "/" + specification;
			}
			identifier_ = identifier_.replaceAll ("//", "/");
		} else {
			identifier_ = reference.identifier + "/" + specification;
		}
		if (!ConfigurationIdentifier.IDENTIFIER_PATTERN.matcher (identifier_).matches ()) {
			throw new IllegalArgumentException (String.format ("Invalid configuration identifier: `%s`", identifier_));
		}
		identifier = identifier_.intern ();
		synchronized (ConfigurationIdentifier.IDENTIFIERS) {
			if (ConfigurationIdentifier.IDENTIFIERS.containsKey (identifier)) {
				parameterIdentifier = ConfigurationIdentifier.IDENTIFIERS.get (identifier);
			} else {
				parameterIdentifier = new ConfigurationIdentifier (isAbsolute, identifier);
				ConfigurationIdentifier.IDENTIFIERS.put (parameterIdentifier.identifier, parameterIdentifier);
			}
		}
		return parameterIdentifier;
	}
	
	/**
	 * Indicates if this identifier is absolute
	 */
	private final boolean absolute;
	/**
	 * The string identifier.
	 */
	private final String identifier;
	public static final ConfigurationIdentifier ROOT = new ConfigurationIdentifier (true, null);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile ("^/?([a-z]([a-z0-9_.]*[a-z0-9])?/)*([a-z]([a-z0-9_.]*[a-z0-9])?)$", Pattern.DOTALL);
	private static final IdentityHashMap<String, ConfigurationIdentifier> IDENTIFIERS = new IdentityHashMap<String, ConfigurationIdentifier> ();
}
