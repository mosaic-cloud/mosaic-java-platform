
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.base.Preconditions;


public class NumericConfigurationParameter<_Value_ extends Number>
			extends SimpleConfigurationParameter<_Value_>
{
	protected NumericConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final _Value_ defaultValue) {
		super (identifier, valueClass, defaultValue, true);
	}
	
	public double resolveDouble (final ConfigurationSource source) {
		Preconditions.checkArgument (Double.class.isAssignableFrom (this.valueClass) || Float.class.isAssignableFrom (this.valueClass) || Long.class.isAssignableFrom (this.valueClass) || Integer.class.isAssignableFrom (this.valueClass));
		final _Value_ value = this.resolve (source);
		return (value.doubleValue ());
	}
	
	public float resolveFloat (final ConfigurationSource source) {
		Preconditions.checkArgument (Float.class.isAssignableFrom (this.valueClass) || Long.class.isAssignableFrom (this.valueClass) || Integer.class.isAssignableFrom (this.valueClass));
		final _Value_ value = this.resolve (source);
		return (value.floatValue ());
	}
	
	public int resolveInt (final ConfigurationSource source) {
		Preconditions.checkArgument (Integer.class.isAssignableFrom (this.valueClass));
		final _Value_ value = this.resolve (source);
		return (value.intValue ());
	}
	
	public long resolveLong (final ConfigurationSource source) {
		Preconditions.checkArgument (Long.class.isAssignableFrom (this.valueClass) || Integer.class.isAssignableFrom (this.valueClass));
		final _Value_ value = this.resolve (source);
		return (value.longValue ());
	}
	
	public static final <_Value_ extends Number> NumericConfigurationParameter<_Value_> create (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		return (NumericConfigurationParameter.create (identifier, valueClass, null));
	}
	
	public static final <_Value_ extends Number> NumericConfigurationParameter<_Value_> create (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final _Value_ defaultValue) {
		return (new NumericConfigurationParameter<_Value_> (identifier, valueClass, defaultValue));
	}
	
	public static final <_Value_ extends Number> NumericConfigurationParameter<_Value_> create (final String identifier, final Class<_Value_> valueClass) {
		return (NumericConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), valueClass));
	}
	
	public static final <_Value_ extends Number> NumericConfigurationParameter<_Value_> create (final String identifier, final Class<_Value_> valueClass, final _Value_ defaultValue) {
		return (NumericConfigurationParameter.create (ConfigurationIdentifier.resolveRelative (identifier), valueClass, defaultValue));
	}
}
