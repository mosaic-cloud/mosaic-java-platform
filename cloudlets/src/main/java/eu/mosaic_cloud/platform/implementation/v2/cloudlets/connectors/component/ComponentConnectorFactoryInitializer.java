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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.component;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorVariant;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorsFactoryBuilderInitializer;

import com.google.common.base.Preconditions;


public final class ComponentConnectorFactoryInitializer
			extends BaseConnectorsFactoryInitializer
{
	public ComponentConnectorFactoryInitializer (final eu.mosaic_cloud.platform.v2.connectors.component.ComponentConnector backingConnector) {
		super ();
		Preconditions.checkNotNull (backingConnector);
		this.backingConnector = backingConnector;
	}
	
	@Override
	protected void initialize_1 (final ConnectorsFactoryBuilderInitializer builder, final CloudletController<?> cloudlet, final ConnectorEnvironment environment, final ConnectorsFactory delegate) {
		this.register (builder, eu.mosaic_cloud.platform.v2.cloudlets.connectors.component.ComponentConnectorFactory.class, ComponentConnectorFactoryInitializer.variant, true, true, new ComponentConnectorFactory (cloudlet, this.backingConnector, environment, delegate));
	}
	
	private final eu.mosaic_cloud.platform.v2.connectors.component.ComponentConnector backingConnector;
	public static final ConnectorVariant variant = ConnectorVariant.resolve ("eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors");
}
