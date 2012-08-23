
package eu.mosaic_cloud.cloudlets.tools;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorsFactoryInitializer;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactoryInitializer
		extends Object
		implements
			IConnectorsFactoryInitializer
{
	protected BaseConnectorsFactoryInitializer ()
	{
		super ();
	}
	
	@Override
	public final void initialize (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (builder);
		Preconditions.checkNotNull (cloudlet);
		Preconditions.checkNotNull (environment);
		this.initialize_1 (builder, cloudlet, environment, delegate);
	}
	
	protected abstract void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate);
}
