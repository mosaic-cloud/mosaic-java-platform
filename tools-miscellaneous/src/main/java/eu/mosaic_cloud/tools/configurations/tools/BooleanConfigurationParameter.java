
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;


public class BooleanConfigurationParameter
			extends SimpleConfigurationParameter<Boolean>
{
	protected BooleanConfigurationParameter (final ConfigurationIdentifier identifier, final Boolean defaultValue) {
		super (identifier, Boolean.class, defaultValue, true);
	}
	
	public boolean resolveBoolean (final ConfigurationSource source) {
		final Boolean value = this.resolve (source);
		return (value.booleanValue ());
	}
	
	public static final BooleanConfigurationParameter create (final ConfigurationIdentifier identifier) {
		return (BooleanConfigurationParameter.create (identifier, (Boolean) null));
	}
	
	public static final BooleanConfigurationParameter create (final ConfigurationIdentifier identifier, final Boolean defaultValue) {
		return (new BooleanConfigurationParameter (identifier, defaultValue));
	}
	
	public static final BooleanConfigurationParameter create (final String identifier) {
		return (BooleanConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier)));
	}
	
	public static final BooleanConfigurationParameter create (final String identifier, final Boolean defaultValue) {
		return (BooleanConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), defaultValue));
	}
}
