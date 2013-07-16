
package eu.mosaic_cloud.cloudlets.v1.connectors.core;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;

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
