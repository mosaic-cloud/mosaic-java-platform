
package eu.mosaic_cloud.connectors.core;


import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;


public interface IConnectorsFactoryInitializer
{
	public abstract void initialize (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate);
}
