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

package eu.mosaic_cloud.platform.v2.cloudlets.core;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorsFactory;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackPassthrough;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


public interface CloudletController<TContext extends Object>
			extends
				Callbacks,
				ConnectorsFactory
{
	public abstract CallbackCompletion<Void> destroy ();
	
	@CallbackPassthrough
	public abstract ConfigurationSource getConfiguration ();
	
	@Override
	@CallbackPassthrough
	public abstract <Factory extends ConnectorFactory<?>> Factory getConnectorFactory (Class<Factory> factory);
	
	@CallbackPassthrough
	public abstract CloudletState getState ();
	
	@CallbackPassthrough
	public abstract ThreadingContext getThreadingContext ();
}
