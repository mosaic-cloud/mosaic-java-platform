
package eu.mosaic_cloud.tools.configurations.tools;


import eu.mosaic_cloud.tools.configurations.tools.BaseConfigurationParameter.Constraint;

import com.google.common.base.Preconditions;


public class NotNullConfigurationParameterConstraint
			extends Object
			implements
				Constraint<Object>
{
	@Override
	public void enforce (final Object value)
				throws IllegalArgumentException {
		Preconditions.checkNotNull (value);
	}
	
	public static final NotNullConfigurationParameterConstraint defaultInstance = new NotNullConfigurationParameterConstraint ();
}
