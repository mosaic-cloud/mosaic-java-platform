
package eu.mosaic_cloud.platform.implementation.v2.configuration;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;


public class ConfigUtils
{
	public static <_Value_ extends Object> _Value_ resolveParameter (final ConfigurationSource config, final String parameter, final Class<_Value_> valueClass, final _Value_ defaultValue) {
		final ConfigurationIdentifier identifier = ConfigurationIdentifier.resolveRelative (parameter);
		final ConfigurationSource.Value<_Value_> outcome = config.resolve (identifier, valueClass);
		switch (outcome.resolution) {
			case Resolved :
			case Default :
				return (outcome.value);
			case Unknown :
				return (defaultValue);
			default :
				throw (new AssertionError ());
		}
	}
}
