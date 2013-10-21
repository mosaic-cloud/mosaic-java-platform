
package eu.mosaic_cloud.tools.configurations.implementations.basic;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public abstract class StringEncodedConfigurationSource
			extends BaseConfigurationSource
{
	protected StringEncodedConfigurationSource () {
		super ();
	}
	
	protected <_Value_ extends Object> _Value_ decodeValue (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final String encodedValue) {
		final ValueDecoder<_Value_> valueDecoder = this.resolveValueDecoder (identifier, valueClass);
		Preconditions.checkNotNull (valueDecoder);
		final _Value_ decodedValue = valueDecoder.decodeValue (identifier, encodedValue);
		return (decodedValue);
	}
	
	protected abstract String resolveEncodedValue (final ConfigurationIdentifier identifier);
	
	@Override
	protected <_Value_ extends Object> Value<_Value_> resolveValue (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		final String encodedValue = this.resolveEncodedValue (identifier);
		if (encodedValue == null)
			return (null);
		final _Value_ decodedValue = this.decodeValue (identifier, valueClass, encodedValue);
		return (Value.createResolved (valueClass, decodedValue));
	}
	
	protected <_Value_ extends Object> ValueDecoder<_Value_> resolveValueDecoder (@SuppressWarnings ("unused") final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		return (PrimitiveValueDecoder.resolve (valueClass));
	}
	
	public static class PrimitiveValueDecoder<_Value_ extends Object>
				extends Object
				implements
					ValueDecoder<_Value_>
	{
		protected PrimitiveValueDecoder (final Class<_Value_> valueClass) {
			super ();
			Preconditions.checkNotNull (valueClass);
			this.valueClass = valueClass;
		}
		
		@Override
		public _Value_ decodeValue (final ConfigurationIdentifier identifier, final String encodedValue) {
			final Object value;
			if (this.valueClass == String.class)
				value = encodedValue;
			else if (this.valueClass == Integer.class)
				value = Integer.valueOf (Integer.parseInt (encodedValue));
			else if (this.valueClass == Long.class)
				value = Long.valueOf (Long.parseLong (encodedValue));
			else if (this.valueClass == Float.class)
				value = Float.valueOf (Float.parseFloat (encodedValue));
			else if (this.valueClass == Double.class)
				value = Double.valueOf (Double.parseDouble (encodedValue));
			else if (this.valueClass == Boolean.class)
				value = Boolean.valueOf (Boolean.parseBoolean (encodedValue));
			else
				throw (new Error ());
			return (this.valueClass.cast (value));
		}
		
		protected final Class<_Value_> valueClass;
		
		public static final <_Value_ extends Object> PrimitiveValueDecoder<_Value_> create (final Class<_Value_> valueClass) {
			return (new PrimitiveValueDecoder<_Value_> (valueClass));
		}
		
		@SuppressWarnings ("unchecked")
		public static final <_Value_ extends Object> PrimitiveValueDecoder<_Value_> resolve (final Class<_Value_> valueClass) {
			return ((PrimitiveValueDecoder<_Value_>) PrimitiveValueDecoder.defaultInstances.get (valueClass));
		}
		
		static {
			final ImmutableMap.Builder<Class<?>, PrimitiveValueDecoder<?>> builder = new ImmutableMap.Builder<Class<?>, PrimitiveValueDecoder<?>> ();
			builder.put (String.class, PrimitiveValueDecoder.create (String.class));
			builder.put (Integer.class, PrimitiveValueDecoder.create (Integer.class));
			builder.put (Long.class, PrimitiveValueDecoder.create (Long.class));
			builder.put (Float.class, PrimitiveValueDecoder.create (Float.class));
			builder.put (Double.class, PrimitiveValueDecoder.create (Double.class));
			builder.put (Boolean.class, PrimitiveValueDecoder.create (Boolean.class));
			defaultInstances = builder.build ();
		}
		public static final ImmutableMap<Class<?>, PrimitiveValueDecoder<?>> defaultInstances;
	}
	
	public interface ValueDecoder<_Value_ extends Object>
	{
		public abstract _Value_ decodeValue (final ConfigurationIdentifier identifier, final String encodedValue);
	}
}
