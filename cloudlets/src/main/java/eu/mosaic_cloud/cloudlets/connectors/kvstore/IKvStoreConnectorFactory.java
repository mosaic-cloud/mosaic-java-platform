
package eu.mosaic_cloud.cloudlets.connectors.kvstore;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IKvStoreConnectorFactory
		extends
			IConnectorFactory<IKvStoreConnector<?, ?, ?>>
{
	<Context, Value, Extra> IKvStoreConnector<Context, Value, Extra> create (IConfiguration configuration, Class<Value> valueClass, DataEncoder<? super Value> valueEncoder, IKvStoreConnectorCallback<Context, Value, Extra> callback, Context callbackContext);
}
