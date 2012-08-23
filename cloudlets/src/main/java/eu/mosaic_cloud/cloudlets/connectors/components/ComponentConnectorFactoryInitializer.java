
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;

import com.google.common.base.Preconditions;


public final class ComponentConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	public ComponentConnectorFactoryInitializer (final eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector backingConnector)
	{
		super ();
		Preconditions.checkNotNull (backingConnector);
		this.backingConnector = backingConnector;
	}
	
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IComponentConnectorFactory.class, new ComponentConnectorFactory (cloudlet, this.backingConnector, environment, delegate));
	}
	
	private final eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector backingConnector;
}
