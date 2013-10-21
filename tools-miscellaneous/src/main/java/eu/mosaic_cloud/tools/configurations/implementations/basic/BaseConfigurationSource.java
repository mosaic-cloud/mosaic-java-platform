
package eu.mosaic_cloud.tools.configurations.implementations.basic;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.base.Preconditions;


public abstract class BaseConfigurationSource
			extends Object
			implements
				ConfigurationSource
{
	protected BaseConfigurationSource () {
		super ();
	}
	
	@Override
	public <_Value_ extends Object> Value<_Value_> resolve (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		Preconditions.checkNotNull (identifier);
		Preconditions.checkNotNull (valueClass);
		final ConfigurationIdentifier resolvedIdentifier = this.resolveIdentifier (identifier);
		Preconditions.checkNotNull (resolvedIdentifier);
		final Value<_Value_> resolvedValue = this.resolveValue (resolvedIdentifier, valueClass);
		final Value<_Value_> value;
		if (resolvedValue != null)
			value = resolvedValue;
		else {
			final Value<_Value_> defaultValue = this.resolveDefaultValue (resolvedIdentifier, valueClass);
			if (defaultValue != null)
				value = resolvedValue;
			else
				value = Value.createUnknown (valueClass);
		}
		Preconditions.checkArgument (value.valueClass == valueClass);
		return (value);
	}
	
	@Override
	public ConfigurationSource splice (final ConfigurationIdentifier identifier) {
		Preconditions.checkNotNull (identifier);
		final ConfigurationIdentifier resolvedIdentifier = this.resolveIdentifier (identifier);
		Preconditions.checkNotNull (resolvedIdentifier);
		final ConfigurationSource splicedConfiguration = this.splice_ (resolvedIdentifier);
		Preconditions.checkNotNull (splicedConfiguration);
		return (splicedConfiguration);
	}
	
	protected <_Value_ extends Object> Value<_Value_> resolveDefaultValue (@SuppressWarnings ("unused") final ConfigurationIdentifier identifier, @SuppressWarnings ("unused") final Class<_Value_> valueClass) {
		return (null);
	}
	
	protected ConfigurationIdentifier resolveIdentifier (final ConfigurationIdentifier identifier) {
		return (identifier);
	}
	
	protected abstract <_Value_ extends Object> Value<_Value_> resolveValue (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass);
	
	protected ConfigurationSource splice_ (final ConfigurationIdentifier identifier) {
		return (SplicedConfigurationSource.create (this, identifier));
	}
}
