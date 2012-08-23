
package eu.mosaic_cloud.cloudlets.connectors.httpg;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

import com.google.common.base.Preconditions;


public final class HttpgQueueConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		Preconditions.checkNotNull (delegate);
		builder.register (IHttpgQueueConnectorFactory.class, new IHttpgQueueConnectorFactory () {
			@Override
			public <TContext, TRequestBody, TResponseBody, TExtra> IHttpgQueueConnector<TRequestBody, TResponseBody, TExtra> create (final IConfiguration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final IHttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback, final TContext callbackContext)
			{
				final HttpgQueueConnector.Callback<TRequestBody, TResponseBody> backingCallback = new HttpgQueueConnector.Callback<TRequestBody, TResponseBody> ();
				final eu.mosaic_cloud.connectors.httpg.IHttpgQueueConnector<TRequestBody, TResponseBody> backingConnector = delegate.getConnectorFactory (eu.mosaic_cloud.connectors.httpg.IHttpgQueueConnectorFactory.class).create (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, backingCallback);
				return new HttpgQueueConnector<TContext, TRequestBody, TResponseBody, TExtra> (cloudlet, backingConnector, configuration, callback, callbackContext, backingCallback);
			}
		});
	}
	
	public static final HttpgQueueConnectorFactoryInitializer defaultInstance = new HttpgQueueConnectorFactoryInitializer ();
}
