
package eu.mosaic_cloud.cloudlets.v1.connectors.core;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;


public abstract class ConnectorOperationSucceededArguments<TExtra extends Object>
			extends ConnectorSucceededArguments
{
	protected ConnectorOperationSucceededArguments (final CloudletController<?> cloudlet, final Connector connector, final TExtra extra) {
		super (cloudlet, connector);
		this.extra = extra;
	}
	
	public final TExtra extra;
}
