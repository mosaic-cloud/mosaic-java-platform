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

package eu.mosaic_cloud.connectors.tools;


import eu.mosaic_cloud.platform.interop.idl.ChannelData;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;


/**
 * Interface for callbacks called by the {@link ConnectorComponentResourceFinder} when a resource
 * is found or not.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IConnectorResourceFinderCallback
		extends
			Callbacks
{
	/**
	 * Called when the resource driver is found
	 * 
	 * @param channel
	 *            interoperability channel parameters (used for communicating
	 *            with the driver)
	 * @throws Throwable
	 */
	public abstract CallbackCompletion<Void> resourceFound (ChannelData channel);
	
	/**
	 * Called when a resource driver cannot be found.
	 */
	public abstract CallbackCompletion<Void> resourceNotFound ();
}