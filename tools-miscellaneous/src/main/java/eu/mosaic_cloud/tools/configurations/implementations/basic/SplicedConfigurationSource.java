
package eu.mosaic_cloud.tools.configurations.implementations.basic;


import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;

import com.google.common.base.Preconditions;


public class SplicedConfigurationSource
			extends BaseConfigurationSource
{
	protected SplicedConfigurationSource (final ConfigurationSource delegate, final ConfigurationIdentifier anchor) {
		super ();
		Preconditions.checkNotNull (delegate);
		Preconditions.checkNotNull (anchor);
		Preconditions.checkArgument ((anchor.type == ConfigurationIdentifier.Type.Absolute) || (anchor.type == ConfigurationIdentifier.Type.Relative));
		this.delegate = delegate;
		this.anchor = anchor;
	}
	
	@Override
	protected ConfigurationIdentifier resolveIdentifier (final ConfigurationIdentifier identifier) {
		return (this.anchor.resolve (identifier));
	}
	
	@Override
	protected <_Value_> Value<_Value_> resolveValue (final ConfigurationIdentifier identifier, final Class<_Value_> valueClass) {
		return (this.delegate.resolve (identifier, valueClass));
	}
	
	protected final ConfigurationIdentifier anchor;
	protected final ConfigurationSource delegate;
	
	public static final SplicedConfigurationSource create (final ConfigurationSource delegate, final ConfigurationIdentifier anchor) {
		return (new SplicedConfigurationSource (delegate, anchor));
	}
}
