
package eu.mosaic_cloud.platform.implementation.v2.connectors.riak;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;
import eu.mosaic_cloud.platform.v2.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;


public final class RiakKvStoreConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	@Override
	protected final void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		builder.register (KvStoreConnectorFactory.class, new KvStoreConnectorFactory () {
			@Override
			public <TValue> RiakKvStoreConnector<TValue> create (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder) {
				return RiakKvStoreConnector.create (ConnectorConfiguration.create (configuration, environment), valueEncoder);
			}
		});
	}
	
	public static final RiakKvStoreConnectorFactoryInitializer defaultInstance = new RiakKvStoreConnectorFactoryInitializer ();
}
