/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.connectors.core;


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
