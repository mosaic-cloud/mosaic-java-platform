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

package eu.mosaic_cloud.cloudlets.v1.cloudlets;


import eu.mosaic_cloud.cloudlets.v1.connectors.core.YYY_core_ConnectorsFactory;
import eu.mosaic_cloud.connectors.v1.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackPassthrough;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * Interface for cloudlet control operations. Each cloudlet has access to an object implementing this interface and uses it to
 * ask for resources or destroying them when they are not required anymore.
 * 
 * @author Georgiana Macariu
 */
public interface CloudletController<Context>
			extends
				Callbacks,
				YYY_core_ConnectorsFactory
{
	CallbackCompletion<Void> destroy ();
	
	@CallbackPassthrough
	Configuration getConfiguration ();
	
	@Override
	@CallbackPassthrough
	<Factory extends ConnectorFactory<?>> Factory getConnectorFactory (Class<Factory> factory);
	
	@CallbackPassthrough
	CloudletState getState ();
	
	@CallbackPassthrough
	ThreadingContext getThreadingContext ();
}
