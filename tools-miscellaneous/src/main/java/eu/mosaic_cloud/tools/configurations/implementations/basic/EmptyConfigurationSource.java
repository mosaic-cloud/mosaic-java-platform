
package eu.mosaic_cloud.tools.configurations.implementations.basic;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;


public final class EmptyConfigurationSource
			extends BaseConfigurationSource
{
	protected EmptyConfigurationSource () {
		super ();
	}
	
	@Override
	protected final <_Value_> Value<_Value_> resolveValue (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		return (null);
	}
	
	public static final EmptyConfigurationSource create () {
		return (new EmptyConfigurationSource ());
	}
	
	public static final EmptyConfigurationSource defaultInstance = EmptyConfigurationSource.create ();
}
