
package eu.mosaic_cloud.connectors.core;


public interface IConnectorsFactoryBuilder
{
	public abstract IConnectorsFactory build ();
	
	public abstract <TFactory extends IConnectorFactory<?>> void register (final Class<TFactory> factoryClass, final TFactory factory);
}
