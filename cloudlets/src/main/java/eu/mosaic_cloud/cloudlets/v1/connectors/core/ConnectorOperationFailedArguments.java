
package eu.mosaic_cloud.cloudlets.v1.connectors.core;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;


public abstract class ConnectorOperationFailedArguments<TExtra extends Object>
			extends ConnectorFailedArguments
{
	protected ConnectorOperationFailedArguments (final CloudletController<?> cloudlet, final Connector connector, final Throwable error, final TExtra extra) {
		super (cloudlet, connector, error);
		this.extra = extra;
	}
	
	public final TExtra extra;
}
