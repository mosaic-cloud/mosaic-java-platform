
package eu.mosaic_cloud.tools.configurations.implementations.basic;


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
	
	@Override
	protected String resolveEncodedValue (final ConfigurationIdentifier identifier) {
		final String propertyName = this.resolvePropertyName (identifier);
		final String encodedValue = this.properties.getProperty (propertyName);
		return (encodedValue);
	}
	
	protected String resolvePropertyName (final ConfigurationIdentifier identifier) {
		if (this.prefix == null)
			return (identifier.path);
		return (this.prefix + identifier.path);
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
	
	public static final PropertiesBackedConfigurationSource create (final Properties properties) {
		return (PropertiesBackedConfigurationSource.create (properties, null));
	}
	
	public static final PropertiesBackedConfigurationSource create (final Properties properties, final String prefix) {
		return (new PropertiesBackedConfigurationSource (properties, prefix));
	}
}
