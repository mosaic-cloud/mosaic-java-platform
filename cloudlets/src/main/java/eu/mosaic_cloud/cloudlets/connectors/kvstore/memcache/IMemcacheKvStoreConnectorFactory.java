
package eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache;


import eu.mosaic_cloud.cloudlets.connectors.kvstore.IKvStoreConnectorCallback;

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IMemcacheKvStoreConnectorFactory
		extends
			IConnectorFactory<IMemcacheKvStoreConnector<?, ?, ?>>
{
	<Context, Value, Extra> IMemcacheKvStoreConnector<Context, Value, Extra> create (IConfiguration configuration, Class<Value> valueClass, DataEncoder<? super Value> dataEncoder, IKvStoreConnectorCallback<Context, Value, Extra> callback, Context callbackContext);
}
