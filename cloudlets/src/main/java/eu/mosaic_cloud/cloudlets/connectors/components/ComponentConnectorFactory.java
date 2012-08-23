
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;

import com.google.common.base.Preconditions;


public final class ComponentConnectorFactory
		extends BaseConnectorFactory<IComponentConnector<?>>
		implements
			IComponentConnectorFactory
{
	public ComponentConnectorFactory (final ICloudletController<?> cloudlet, final eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector backingConnector, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		super (cloudlet, environment, delegate);
		Preconditions.checkNotNull (backingConnector);
		this.backingConnector = backingConnector;
	}
	
	@Override
	public final <TConnectorContext, TExtra> IComponentConnector<TExtra> create (final IComponentConnectorCallbacks<TConnectorContext, TExtra> callbacks, final TConnectorContext callbacksContext)
	{
		final IConfiguration configuration = PropertyTypeConfiguration.createEmpty ();
		final IComponentConnector<TExtra> connector = new ComponentConnector<TConnectorContext, TExtra> (this.cloudlet, this.backingConnector, configuration, callbacks, callbacksContext);
		return connector;
	}
	
	private final eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector backingConnector;
}
