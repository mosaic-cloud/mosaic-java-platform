
package eu.mosaic_cloud.cloudlets.v1.connectors.core;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;


public abstract class ConnectorSucceededArguments
			extends ConnectorCallbackArguments
{
	protected ConnectorSucceededArguments (final CloudletController<?> cloudlet, final Connector connector) {
		super (cloudlet, connector);
	}
}
