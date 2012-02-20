
package eu.mosaic_cloud.cloudlets.connectors.core;


public interface IConnectorsFactory
{
	<F extends IConnectorFactory<?>> F getConnectorFactory (Class<F> factory);
}
