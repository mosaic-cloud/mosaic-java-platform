
package eu.mosaic_cloud.connectors.httpg;


import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;


public final class HttpgQueueConnectorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IHttpgQueueConnectorFactory.class, new IHttpgQueueConnectorFactory () {
			@Override
			public <TRequestBody, TResponseBody> IHttpgQueueConnector<TRequestBody, TResponseBody> create (final IConfiguration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final IHttpgQueueCallback<TRequestBody, TResponseBody> callback)
			{
				return HttpgQueueConnector.create (ConnectorConfiguration.create (configuration, environment), requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback);
			}
		});
	}
	
	public static final HttpgQueueConnectorFactoryInitializer defaultInstance = new HttpgQueueConnectorFactoryInitializer ();
}
