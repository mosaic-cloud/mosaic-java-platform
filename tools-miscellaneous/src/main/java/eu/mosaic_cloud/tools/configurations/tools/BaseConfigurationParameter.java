
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier.Type;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource.Value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


public abstract class BaseConfigurationParameter<_Value_ extends Object>
			extends Object
{
	protected BaseConfigurationParameter (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass, final Iterable<? extends Constraint<? super _Value_>> constraints) {
		super ();
		Preconditions.checkNotNull (identifier);
		Preconditions.checkArgument (identifier.type == Type.Relative);
		Preconditions.checkNotNull (valueClass);
		this.identifier = identifier;
		this.valueClass = valueClass;
		if (constraints != null)
			this.constraints = ImmutableList.copyOf (constraints);
		else
			this.constraints = ImmutableList.of ();
	}
	
	public _Value_ resolve (final ConfigurationSource source) {
		Preconditions.checkNotNull (source);
		final Value<_Value_> resolution = this.resolveValue (source);
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
	
	protected Value<_Value_> resolveValue (final ConfigurationSource source) {
		return (source.resolve (this.identifier, this.valueClass));
	}
	
	protected boolean validateValue (final _Value_ value) {
		for (final Constraint<? super _Value_> constraint : this.constraints)
			try {
				constraint.enforce (value);
			} catch (final IllegalArgumentException exception) {
				return (false);
			}
		return (true);
	}
	
	protected final ImmutableList<Constraint<? super _Value_>> constraints;
	protected final ConfigurationIdentifier identifier;
	protected final Class<_Value_> valueClass;
	
	public static interface Constraint<_Value_ extends Object>
	{
		public abstract void enforce (final _Value_ value)
					throws IllegalArgumentException;
	}
}
