
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;


public class StringConfigurationParameter
			extends SimpleConfigurationParameter<String>
{
	protected StringConfigurationParameter (final ConfigurationIdentifier identifier, final String defaultValue, final boolean notNullConstraint) {
		super (identifier, String.class, defaultValue, notNullConstraint);
	}
	
	public static final StringConfigurationParameter create (final ConfigurationIdentifier identifier) {
		return (StringConfigurationParameter.create (identifier, null, true));
	}
	
	public static final StringConfigurationParameter create (final ConfigurationIdentifier identifier, final String defaultValue, final boolean notNullConstraint) {
		return (new StringConfigurationParameter (identifier, defaultValue, notNullConstraint));
	}
	
	public static final StringConfigurationParameter create (final String identifier) {
		return (StringConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier)));
	}
	
	public static final StringConfigurationParameter create (final String identifier, final String defaultValue, final boolean notNullConstraint) {
		return (StringConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), defaultValue, notNullConstraint));
	}
}
