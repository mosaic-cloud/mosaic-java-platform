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


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;


/**
 * This class implements a configuration handler for project configurations
 * based on property files.
 * 
 * @author CiprianCraciun, Georgiana Macariu
 * 
 */
public final class PropertyTypeConfiguration
		implements
			IConfiguration
{
	private PropertyTypeConfiguration (final Properties properties)
	{
		super ();
		this.properties = properties;
		this.root = ConfigurationIdentifier.ROOT;
	}
	
	private PropertyTypeConfiguration (final Properties properties, final ConfigurationIdentifier root)
	{
		super ();
		this.properties = properties;
		this.root = root;
	}
	
	@Override
	public <T> void addParameter (final ConfigurationIdentifier identifier, final T value)
	{
		final String property = identifier.getIdentifier ();
		this.properties.put (property, value.toString ());
	}
	
	@Override
	public <T> void addParameter (final String property, final T value)
	{
		this.properties.put (property, value.toString ());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (final Object config)
	{
		boolean isEqual;
		if (config == null) {
			isEqual = false;
		} else if (config instanceof PropertyTypeConfiguration) {
			final PropertyTypeConfiguration otherConfig = (PropertyTypeConfiguration) config;
			isEqual = ((this.root != null) && !this.root.equals (otherConfig.root)) || ((this.root == null) && (otherConfig.root != null)) || ((this.properties != null) && !this.properties.equals (otherConfig.properties)) || ((this.properties == null) && (otherConfig.properties != null));
			isEqual ^= true;
		} else {
			isEqual = false;
		}
		return isEqual;
	}
	
	@Override
	public <T extends Object> PropertyTypeParameter<T> getParameter (final ConfigurationIdentifier identifier, final Class<T> valueClass)
	{
		return new PropertyTypeParameter<T> (identifier, valueClass);
	}
	
	@Override
	public ConfigurationIdentifier getRootIdentifier ()
	{
		return this.root;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode ()
	{
		final int prime = 31; // NOPMD
		int result = 1; // NOPMD
		result = (prime * result) + ((this.properties == null) ? 0 : this.properties.hashCode ());
		result = (prime * result) + ((this.root == null) ? 0 : this.root.hashCode ());
		return result;
	}
	
	@Override
	public PropertyTypeConfiguration spliceConfiguration (final ConfigurationIdentifier relative)
	{
		ConfigurationIdentifier root;
		if (relative.isAbsolute ()) {
			root = relative;
		} else {
			root = this.root.resolve (relative);
		}
		return new PropertyTypeConfiguration (this.properties, root);
	}
	
	@Override
	public String toString ()
	{
		final StringBuilder builder = new StringBuilder ();
		for (final Object key : this.properties.keySet ()) {
			builder.append (key.toString () + ": " + this.properties.getProperty (key.toString ()) + "\n");
		}
		return builder.toString ();
	}
	
	private <T extends Object> T decodeValue (final Class<T> valueClass, final String encodedValue)
	{
		T value;
		if (valueClass == String.class) {
			value = valueClass.cast (encodedValue);
		} else if (valueClass == Boolean.class) {
			value = valueClass.cast (Boolean.parseBoolean (encodedValue));
		} else if (valueClass == Integer.class) {
			value = valueClass.cast (Integer.parseInt (encodedValue));
		} else if (valueClass == Long.class) {
			value = valueClass.cast (Long.parseLong (encodedValue));
		} else if (valueClass == Double.class) {
			value = valueClass.cast (Double.parseDouble (encodedValue));
		} else if (valueClass == Float.class) {
			value = valueClass.cast (Float.parseFloat (encodedValue));
		} else if (valueClass == Character.class) {
			if (encodedValue.length () != 1) {
				throw new IllegalArgumentException ();
			}
			value = valueClass.cast (encodedValue.charAt (0));
		} else {
			throw new IllegalAccessError ();
		}
		return value;
	}
	
	private String selectEncodedValue (final ConfigurationIdentifier identifier)
	{
		String key_;
		if (identifier.isAbsolute ()) {
			if (identifier == ConfigurationIdentifier.ROOT) {
				key_ = "";
			} else {
				key_ = identifier.getIdentifier ();
			}
		} else {
			key_ = this.root.resolve (identifier).getIdentifier ();
		}
		final String key = key_.substring (1).replace ('/', '.');
		return this.properties.getProperty (key, null);
	}
	
	public static PropertyTypeConfiguration create ()
	{
		final Properties properties = new Properties (System.getProperties ());
		final PropertyTypeConfiguration configuration = new PropertyTypeConfiguration (properties);
		return configuration;
	}
	
	/**
	 * Creates a configuration object and loads the configuration parameters
	 * from the specified resource file using a specific class loader.
	 * 
	 * @param classLoader
	 *            the class loader used for loading the configuration file
	 * @param resource
	 *            the name of the configuration file
	 * @return the configuration object
	 */
	public static PropertyTypeConfiguration create (final ClassLoader classLoader, final String resource)
	{
		final InputStream stream = classLoader.getResourceAsStream (resource);
		PropertyTypeConfiguration configuration = null; // NOPMD
		if (stream != null) {
			final Properties properties = new Properties (System.getProperties ());
			try {
				properties.load (stream);
				stream.close ();
				configuration = new PropertyTypeConfiguration (properties);
			} catch (final IOException exception) {
				ExceptionTracer.traceIgnored (exception);
			}
		} else {
			throw (new IllegalArgumentException ("resource not found"));
		}
		return configuration;
	}
	
	/**
	 * Creates a configuration object and loads the configuration parameters
	 * from the specified input stream.
	 * 
	 * @param stream
	 *            the input stream from where to load configuration parameters
	 * @return the configuration object
	 * @throws IOException
	 *             if an error occurred when reading from the input stream
	 */
	public static PropertyTypeConfiguration create (final InputStream stream)
			throws IOException
	{
		final Properties properties = new Properties (System.getProperties ());
		if (stream != null) {
			properties.load (stream);
			stream.close ();
		} else {
			throw (new IllegalArgumentException ());
		}
		final PropertyTypeConfiguration configuration = new PropertyTypeConfiguration (properties);
		return configuration;
	}
	
	private final Properties properties;
	private final ConfigurationIdentifier root;
	
	/**
	 * Implements the configuration parameter in property style configuration
	 * object.
	 * 
	 * @author Ciprian Craciun, Georgiana Macariu
	 * 
	 * @param <T>
	 *            the type of the value of the parameter
	 */
	public final class PropertyTypeParameter<T>
			implements
				IConfigurationParameter<T>
	{
		public PropertyTypeParameter (final ConfigurationIdentifier identifier, final Class<T> valueClass)
		{
			super ();
			this.identifier = identifier;
			this.valueClass = valueClass;
		}
		
		@Override
		public ConfigurationIdentifier getIdentifier ()
		{
			return this.identifier;
		}
		
		@Override
		public T getValue (final T defaultValue)
		{
			T value;
			if (this.value == null) {
				if (this.valueClass == IConfiguration.class) {
					this.value = this.valueClass.cast (PropertyTypeConfiguration.this.spliceConfiguration (this.identifier));
				} else {
					final String encodedValue = PropertyTypeConfiguration.this.selectEncodedValue (this.identifier);
					if (encodedValue != null) {
						this.value = PropertyTypeConfiguration.this.decodeValue (this.valueClass, encodedValue);
					}
				}
			}
			if (this.value == null) {
				value = defaultValue;
			} else {
				value = this.value;
			}
			return value;
		}
		
		@Override
		public Class<T> getValueClass ()
		{
			return this.valueClass;
		}
		
		private final ConfigurationIdentifier identifier;
		private T value;
		private final Class<T> valueClass;
	}
}
