
package eu.mosaic_cloud.platform.implementation.v2.connectors.riak;


import eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;


public class RiakKvStoreConnector<TValue extends Object>
			extends BaseKvStoreConnector<TValue, RiakKvStoreConnectorProxy<TValue>>
{
	protected RiakKvStoreConnector (final RiakKvStoreConnectorProxy<TValue> proxy, final ConnectorConfiguration configuration) {
		super (proxy, configuration);
	}
	
	public static <TValue> RiakKvStoreConnector<TValue> create (final ConnectorConfiguration configuration, final DataEncoder<TValue> encoder) {
		return (new RiakKvStoreConnector<TValue> (RiakKvStoreConnectorProxy.create (configuration, encoder), configuration));
	}
}
