
package eu.mosaic_cloud.connectors.kvstore.memcache;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public final class MemcacheKvStoreConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IMemcacheKvStoreConnectorFactory.class, new IMemcacheKvStoreConnectorFactory () {
			@Override
			public <TValue> IMemcacheKvStoreConnector<TValue> create (final IConfiguration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder)
			{
				return MemcacheKvStoreConnector.create (ConnectorConfiguration.create (configuration, environment), valueEncoder);
			}
		});
	}
	
	public static final MemcacheKvStoreConnectorFactoryInitializer defaultInstance = new MemcacheKvStoreConnectorFactoryInitializer ();
}
