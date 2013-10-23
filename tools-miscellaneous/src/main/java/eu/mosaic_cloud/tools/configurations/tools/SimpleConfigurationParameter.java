
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.collect.ImmutableList;


public class SimpleConfigurationParameter<_Value_ extends Object>
			extends BaseConfigurationParameter<_Value_>
{
	protected SimpleConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final _Value_ defaultValue, final boolean notNullConstraint) {
		this (identifier, valueClass, defaultValue, notNullConstraint ? ImmutableList.of (NotNullConfigurationParameterConstraint.defaultInstance) : null);
	}
	
	protected SimpleConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final _Value_ defaultValue, final Iterable<? extends Constraint<? super _Value_>> constraints) {
		super (identifier, valueClass, constraints);
		this.defaultValue = defaultValue;
	}
	
	@Override
	protected _Value_ resolveDefaultValue (final ConfigurationSource source) {
		return (this.defaultValue);
	}
	
	protected final _Value_ defaultValue;
	
	public static final <_Value_ extends Object> SimpleConfigurationParameter<_Value_> create (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		return (SimpleConfigurationParameter.create (identifier, valueClass, null, true));
	}
	
	public static final <_Value_ extends Object> SimpleConfigurationParameter<_Value_> create (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final _Value_ defaultValue, final boolean notNullConstraint) {
		return (new SimpleConfigurationParameter<_Value_> (identifier, valueClass, defaultValue, notNullConstraint));
	}
	
	public static final <_Value_ extends Object> SimpleConfigurationParameter<_Value_> create (final String identifier, final Class<_Value_> valueClass) {
		return (SimpleConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), valueClass));
	}
	
	public static final <_Value_ extends Object> SimpleConfigurationParameter<_Value_> create (final String identifier, final Class<_Value_> valueClass, final _Value_ defaultValue, final boolean notNullConstraint) {
		return (SimpleConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), valueClass, defaultValue, notNullConstraint));
	}
}
