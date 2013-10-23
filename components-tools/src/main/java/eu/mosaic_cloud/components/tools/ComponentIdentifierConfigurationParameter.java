
package eu.mosaic_cloud.components.tools;


import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.tools.NotNullConfigurationParameterConstraint;
import eu.mosaic_cloud.tools.configurations.tools.ParsedConfigurationParameter;

import com.google.common.collect.ImmutableList;


public class ComponentIdentifierConfigurationParameter
			extends ParsedConfigurationParameter<ComponentIdentifier, String>
{
	protected ComponentIdentifierConfigurationParameter (final ConfigurationIdentifier identifier, final ComponentIdentifier defaultValue, final boolean notNullConstraint) {
		this (identifier, defaultValue, notNullConstraint ? ImmutableList.of (NotNullConfigurationParameterConstraint.defaultInstance) : null);
	}
	
	protected ComponentIdentifierConfigurationParameter (final ConfigurationIdentifier identifier, final ComponentIdentifier defaultValue, final Iterable<? extends Constraint<? super ComponentIdentifier>> constraints) {
		super (identifier, ComponentIdentifier.class, String.class, constraints);
		this.defaultValue = defaultValue;
	}
	
	@Override
	protected ComponentIdentifier decodeValue (final String encodedValue) {
		return (ComponentIdentifier.resolve (encodedValue));
	}
	
	@Override
	protected ComponentIdentifier resolveDefaultValue (final ConfigurationSource source) {
		return (this.defaultValue);
	}
	
	protected final ComponentIdentifier defaultValue;
	
	public static final ComponentIdentifierConfigurationParameter create (final ConfigurationIdentifier identifier) {
		return (ComponentIdentifierConfigurationParameter.create (identifier, null, true));
	}
	
	public static final ComponentIdentifierConfigurationParameter create (final ConfigurationIdentifier identifier, final ComponentIdentifier defaultValue, final boolean notNullConstraint) {
		return (new ComponentIdentifierConfigurationParameter (identifier, defaultValue, notNullConstraint));
	}
	
	public static final ComponentIdentifierConfigurationParameter create (final String identifier) {
		return (ComponentIdentifierConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier)));
	}
	
	public static final ComponentIdentifierConfigurationParameter create (final String identifier, final String defaultValue, final boolean notNullConstraint) {
		return (ComponentIdentifierConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), ComponentIdentifier.resolve (defaultValue), notNullConstraint));
	}
}
