
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier.Type;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource.Value;

import com.google.common.base.Preconditions;


public abstract class BaseConfigurationParameter<_Value_ extends Object>
			extends Object
{
	protected BaseConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		super ();
		Preconditions.checkNotNull (identifier);
		Preconditions.checkArgument (identifier.type == Type.Relative);
		Preconditions.checkNotNull (valueClass);
		this.identifier = identifier;
		this.valueClass = valueClass;
	}
	
	public _Value_ resolve (final ConfigurationSource source) {
		Preconditions.checkNotNull (source);
		final Value<_Value_> resolution = source.resolve (this.identifier, this.valueClass);
		final _Value_ value;
		switch (resolution.resolution) {
			case Resolved :
			case Default :
				value = resolution.value;
				break;
			case Unknown :
				value = this.resolveDefaultValue (source);
				break;
			default :
				throw (new AssertionError ());
		}
		if (!this.validateValue (value))
			throw (new IllegalArgumentException ());
		return (this.valueClass.cast (value));
	}
	
	protected abstract _Value_ resolveDefaultValue (final ConfigurationSource source);
	
	protected abstract boolean validateValue (final _Value_ value);
	
	protected final ConfigurationIdentifier identifier;
	protected final Class<_Value_> valueClass;
}
