
package eu.mosaic_cloud.connectors.kvstore.generic;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.IKvStoreConnectorFactory;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public final class GenericKvStoreConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IKvStoreConnectorFactory.class, new IKvStoreConnectorFactory () {
			@Override
			public <TValue> IKvStoreConnector<TValue> create (final IConfiguration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder)
			{
				return GenericKvStoreConnector.create (ConnectorConfiguration.create (configuration, environment), valueEncoder);
			}
		});
	}
	
	public static final GenericKvStoreConnectorFactoryInitializer defaultInstance = new GenericKvStoreConnectorFactoryInitializer ();
}
