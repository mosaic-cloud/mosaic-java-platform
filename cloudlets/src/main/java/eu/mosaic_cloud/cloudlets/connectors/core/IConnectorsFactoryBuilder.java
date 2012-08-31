
package eu.mosaic_cloud.cloudlets.connectors.core;


public interface IConnectorsFactoryBuilder
		extends
			eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder
{
	@Override
	public abstract IConnectorsFactory build ();
	
	public abstract void initialize (final IConnectorsFactoryInitializer initializer);
}
