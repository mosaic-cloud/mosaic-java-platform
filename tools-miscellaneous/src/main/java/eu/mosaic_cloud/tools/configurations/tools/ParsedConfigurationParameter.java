
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource.Value;


public abstract class ParsedConfigurationParameter<_DecodedValue_ extends Object, _EncodedValue_ extends Object>
			extends BaseConfigurationParameter<_DecodedValue_>
{
	protected ParsedConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_DecodedValue_> decodedValueClass, final Class<_EncodedValue_> encodedValueClass, final Iterable<? extends Constraint<? super _DecodedValue_>> constraints) {
		super (identifier, decodedValueClass, constraints);
		this.encodedValueClass = encodedValueClass;
	}
	
	protected abstract _DecodedValue_ decodeValue (final _EncodedValue_ encodedValue);
	
	protected Value<_EncodedValue_> resolveEncodedValue (final ConfigurationSource source) {
		return (source.resolve (this.identifier, this.encodedValueClass));
	}
	
	@Override
	protected Value<_DecodedValue_> resolveValue (final ConfigurationSource source) {
		final Value<_EncodedValue_> encodedValue = this.resolveEncodedValue (source);
		final Value<_DecodedValue_> decodedValue;
		switch (encodedValue.resolution) {
			case Resolved :
				if (encodedValue.hasDefault)
					decodedValue = Value.createResolved (this.valueClass, this.decodeValue (encodedValue.resolvedValue), this.decodeValue (encodedValue.defaultValue));
				else
					decodedValue = Value.createResolved (this.valueClass, this.decodeValue (encodedValue.resolvedValue));
				break;
			case Default :
				decodedValue = Value.createDefault (this.valueClass, this.decodeValue (encodedValue.defaultValue));
				break;
			case Unknown :
				decodedValue = Value.createUnknown (this.valueClass);
				break;
			default :
				throw (new AssertionError ());
		}
		return (decodedValue);
	}
	
	protected final Class<_EncodedValue_> encodedValueClass;
}
