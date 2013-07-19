
package eu.mosaic_cloud.platform.v2.cloudlets.connectors.core;


import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;

import com.google.common.base.Preconditions;


public abstract class ConnectorFailedArguments
			extends ConnectorCallbackArguments
{
	protected ConnectorFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Throwable error) {
		super (cloudlet, connector);
		Preconditions.checkNotNull (error);
		this.error = error;
	}
	
	public final Throwable error;
}
