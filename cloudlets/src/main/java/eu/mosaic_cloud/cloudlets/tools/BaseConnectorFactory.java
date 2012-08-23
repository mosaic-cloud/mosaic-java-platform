
package eu.mosaic_cloud.cloudlets.tools;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnector;
import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorFactory<TConnector extends IConnector>
		extends eu.mosaic_cloud.connectors.tools.BaseConnectorFactory<TConnector>
		implements
			IConnectorFactory<TConnector>
{
	protected BaseConnectorFactory (final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		super (environment, delegate);
		Preconditions.checkNotNull (cloudlet);
		this.cloudlet = cloudlet;
	}
	
	protected final ICloudletController<?> cloudlet;
}
