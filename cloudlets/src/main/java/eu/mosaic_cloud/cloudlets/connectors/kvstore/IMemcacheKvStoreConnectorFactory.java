
package eu.mosaic_cloud.cloudlets.connectors.kvstore;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IMemcacheKvStoreConnectorFactory
		extends
			IConnectorFactory<IMemcacheKvStoreConnector<?, ?>>
{
	<Context, Data> IMemcacheKvStoreConnector<Context, Data> create (IConfiguration configuration, Class<Data> dataClass, DataEncoder<? super Data> dataEncoder);
}
