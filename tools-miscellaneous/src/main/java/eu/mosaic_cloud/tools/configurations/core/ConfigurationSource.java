
package eu.mosaic_cloud.tools.configurations.core;


import com.google.common.base.Preconditions;


public interface ConfigurationSource
{
	public abstract <_Value_ extends Object> Value<_Value_> resolve (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass);
	
	public abstract ConfigurationSource splice (final ConfigurationIdentifier relative);
	
	public enum Resolution
	{
		Default,
		Resolved,
		Unknown;
	}
	
	public static final class Value<_Value_ extends Object>
				extends Object
	{
		private Value (final Class<_Value_> valueClass, final Resolution resolution, final _Value_ resolvedValue, final boolean hasDefault, final _Value_ defaultValue) {
			super ();
			Preconditions.checkNotNull (valueClass);
			if (resolution != Resolution.Resolved)
				Preconditions.checkArgument (resolvedValue == null);
			if (resolution == Resolution.Default)
				Preconditions.checkArgument (hasDefault);
			if (!hasDefault)
				Preconditions.checkArgument (defaultValue == null);
			if (resolvedValue != null)
				Preconditions.checkArgument (valueClass.isInstance (resolvedValue));
			if (defaultValue != null)
				Preconditions.checkArgument (valueClass.isInstance (defaultValue));
			this.valueClass = valueClass;
			this.resolution = resolution;
			this.resolvedValue = resolvedValue;
			this.hasDefault = hasDefault;
			this.defaultValue = defaultValue;
			switch (this.resolution) {
				case Resolved :
					this.value = this.resolvedValue;
					break;
				case Default :
					this.value = this.defaultValue;
					break;
				case Unknown :
					this.value = null;
					break;
				default :
					throw (new IllegalArgumentException ());
			}
		}
		
		public final _Value_ defaultValue;
		public final boolean hasDefault;
		public final Resolution resolution;
		public final _Value_ resolvedValue;
		public final _Value_ value;
		public final Class<_Value_> valueClass;
		
		public static final <_Value_ extends Object> Value<_Value_> createDefault (final Class<_Value_> valueClass, final _Value_ defaultValue) {
			return (new Value<_Value_> (valueClass, Resolution.Default, null, true, defaultValue));
		}
		
		public static final <_Value_ extends Object> Value<_Value_> createResolved (final Class<_Value_> valueClass, final _Value_ resolvedValue) {
			return (new Value<_Value_> (valueClass, Resolution.Resolved, resolvedValue, false, null));
		}
		
		public static final <_Value_ extends Object> Value<_Value_> createResolved (final Class<_Value_> valueClass, final _Value_ resolvedValue, final _Value_ defaultValue) {
			return (new Value<_Value_> (valueClass, Resolution.Resolved, resolvedValue, true, defaultValue));
		}
		
		public static final <_Value_ extends Object> Value<_Value_> createUnknown (final Class<_Value_> valueClass) {
			return (new Value<_Value_> (valueClass, Resolution.Unknown, null, false, null));
		}
	}
}
