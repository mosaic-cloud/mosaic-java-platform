
package eu.mosaic_cloud.cloudlets.connectors.core;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;


public interface IConnectorsFactoryInitializer
{
	public abstract void initialize (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate);
}
