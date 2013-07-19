
package eu.mosaic_cloud.platform.v2.cloudlets.connectors.core;


import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;


public abstract class ConnectorOperationFailedArguments<TExtra extends Object>
			extends ConnectorFailedArguments
{
	protected ConnectorOperationFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Throwable error, final TExtra extra) {
		super (cloudlet, connector, error);
		this.extra = extra;
	}
	
	public final TExtra extra;
}
