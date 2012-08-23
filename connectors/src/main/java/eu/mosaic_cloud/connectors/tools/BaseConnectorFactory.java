
package eu.mosaic_cloud.connectors.tools;


import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorFactory<TConnector extends IConnector>
		extends Object
		implements
			IConnectorFactory<TConnector>
{
	protected BaseConnectorFactory (final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		super ();
		Preconditions.checkNotNull (environment);
		this.environment = environment;
		this.delegate = delegate;
	}
	
	protected final IConnectorsFactory delegate;
	protected final ConnectorEnvironment environment;
}
