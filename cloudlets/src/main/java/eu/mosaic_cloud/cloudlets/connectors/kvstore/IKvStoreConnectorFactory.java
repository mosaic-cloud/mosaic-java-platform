
package eu.mosaic_cloud.cloudlets.connectors.kvstore;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public interface IKvStoreConnectorFactory
		extends
			IConnectorFactory<IKvStoreConnector<?, ?>>
{
	<Context, Data> IKvStoreConnector<Context, Data> create (IConfiguration configuration, Class<Data> dataClass, DataEncoder<? super Data> dataEncoder, IKvStoreConnectorCallback<Context, Data> callback, Context callbackContext);
}
