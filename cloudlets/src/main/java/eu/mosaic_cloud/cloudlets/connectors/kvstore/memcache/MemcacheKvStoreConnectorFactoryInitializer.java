
package eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;


public final class MemcacheKvStoreConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (delegate);
		builder.register (IMemcacheKvStoreConnectorFactory.class, new IMemcacheKvStoreConnectorFactory () {
			@Override
			public <TContext, TValue, TExtra> IMemcacheKvStoreConnector<TValue, TExtra> create (final IConfiguration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext)
			{
				final eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue> backingConnector = (eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue>) delegate.getConnectorFactory (eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnectorFactory.class).create (configuration, valueClass, valueEncoder);
				return new MemcacheKvStoreConnector<TContext, TValue, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext);
			}
		});
	}
	
	public static final MemcacheKvStoreConnectorFactoryInitializer defaultInstance = new MemcacheKvStoreConnectorFactoryInitializer ();
}
