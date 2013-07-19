
package eu.mosaic_cloud.platform.v2.cloudlets.connectors.core;


import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;


public abstract class ConnectorSucceededArguments
			extends ConnectorCallbackArguments
{
	protected ConnectorSucceededArguments (final CloudletController<?> cloudlet, final Connector connector) {
		super (cloudlet, connector);
	}
}
