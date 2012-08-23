
package eu.mosaic_cloud.connectors.tools;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryInitializer;

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
	public final void initialize (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (builder);
		Preconditions.checkNotNull (environment);
		this.initialize_1 (builder, environment, delegate);
	}
	
	protected abstract void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate);
}
