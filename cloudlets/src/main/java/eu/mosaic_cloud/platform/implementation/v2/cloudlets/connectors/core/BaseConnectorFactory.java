/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.Connector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorFactory<TConnector extends Connector>
			extends eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorFactory<TConnector>
			implements
				ConnectorFactory<TConnector>
{
	protected BaseConnectorFactory (final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		super (environment, delegate);
		Preconditions.checkNotNull (cloudlet);
		this.cloudlet = cloudlet;
	}
	
	protected final CloudletController<?> cloudlet;
}
