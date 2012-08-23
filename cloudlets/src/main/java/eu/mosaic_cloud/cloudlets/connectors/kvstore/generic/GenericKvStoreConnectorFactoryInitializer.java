
package eu.mosaic_cloud.cloudlets.connectors.kvstore.generic;


import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;


public final class GenericKvStoreConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (delegate);
		builder.register (IKvStoreConnectorFactory.class, new IKvStoreConnectorFactory () {
			@Override
			public <TContext, TTValue, TExtra> IKvStoreConnector<TTValue, TExtra> create (final IConfiguration configuration, final Class<TTValue> valueClass, final DataEncoder<TTValue> valueEncoder, final IKvStoreConnectorCallback<TContext, TTValue, TExtra> callback, final TContext callbackContext)
			{
				final eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TTValue> backingConnector = (eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector<TTValue>) delegate.getConnectorFactory (eu.mosaic_cloud.connectors.kvstore.IKvStoreConnectorFactory.class).create (configuration, valueClass, valueEncoder);
				return new GenericKvStoreConnector<TContext, TTValue, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final GenericKvStoreConnectorFactoryInitializer defaultInstance = new GenericKvStoreConnectorFactoryInitializer ();
}
