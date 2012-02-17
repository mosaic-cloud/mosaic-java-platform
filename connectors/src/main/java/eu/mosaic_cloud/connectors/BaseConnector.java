
package eu.mosaic_cloud.connectors;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;


public abstract class BaseConnector<_Proxy_ extends BaseConnectorProxy>
		extends Object
		implements
			IConnector
{
	protected BaseConnector (final _Proxy_ proxy)
	{
		super ();
		Preconditions.checkNotNull (proxy);
		this.proxy = proxy;
		this.logger = MosaicLogger.createLogger (this);
	}
	
	protected final MosaicLogger logger;
	protected final _Proxy_ proxy;
}
