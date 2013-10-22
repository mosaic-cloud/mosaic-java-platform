
package eu.mosaic_cloud.tools.configurations.implementations.basic;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;

import com.google.common.base.Preconditions;


public class PropertiesBackedConfigurationSource
			extends StringEncodedConfigurationSource
{
	protected PropertiesBackedConfigurationSource (final Properties properties, final String prefix) {
		super ();
		Preconditions.checkNotNull (properties);
		this.properties = properties;
		this.prefix = prefix;
	}
	
	@Deprecated
	public final void overridePropertyValue (final ConfigurationIdentifier identifier, final String encodedValue) {
		final String propertyName = this.resolvePropertyName (identifier);
		this.properties.setProperty (propertyName, encodedValue);
	}
	
	@Deprecated
	public final void overridePropertyValue (final String propertyName, final String encodedValue) {
		this.properties.setProperty (propertyName, encodedValue);
	}
	
	@Override
	protected String resolveEncodedValue (final ConfigurationIdentifier identifier) {
		final String propertyName = this.resolvePropertyName (identifier);
		final String encodedValue = this.properties.getProperty (propertyName);
		return (encodedValue);
	}
	
	protected String resolvePropertyName (final ConfigurationIdentifier identifier) {
		if (this.prefix == null)
			return (identifier.path.replace ('/', '.'));
		return ((this.prefix + identifier.path).replace ('/', '.'));
	}
	
	protected final String prefix;
	protected final Properties properties;
	
	public static final PropertiesBackedConfigurationSource clone (final Map<String, String> source) {
		return (PropertiesBackedConfigurationSource.clone (source, null));
	}
	
	public static final PropertiesBackedConfigurationSource clone (final Map<String, String> source, final String prefix) {
		final Properties properties = new Properties ();
		for (final Map.Entry<String, String> entry : source.entrySet ())
			properties.setProperty (entry.getKey (), entry.getValue ());
		return (PropertiesBackedConfigurationSource.create (properties, prefix));
	}
	
	public static final PropertiesBackedConfigurationSource create () {
		return (PropertiesBackedConfigurationSource.create (new Properties ()));
	}
	
	public static final PropertiesBackedConfigurationSource create (final Properties properties) {
		return (PropertiesBackedConfigurationSource.create (properties, null));
	}
	
	public static final PropertiesBackedConfigurationSource create (final Properties properties, final String prefix) {
		return (new PropertiesBackedConfigurationSource (properties, prefix));
	}
	
	public static final PropertiesBackedConfigurationSource load (final InputStream source)
				throws IOException {
		return (PropertiesBackedConfigurationSource.load (source, null));
	}
	
	public static final PropertiesBackedConfigurationSource load (final InputStream source, final String prefix)
				throws IOException {
		final Properties properties = new Properties ();
		properties.load (source);
		return (new PropertiesBackedConfigurationSource (properties, prefix));
	}
	
	public static final PropertiesBackedConfigurationSource load (final Reader source)
				throws IOException {
		return (PropertiesBackedConfigurationSource.load (source, null));
	}
	
	public static final PropertiesBackedConfigurationSource load (final Reader source, final String prefix)
				throws IOException {
		final Properties properties = new Properties ();
		properties.load (source);
		return (new PropertiesBackedConfigurationSource (properties, prefix));
	}
}
