/*
 * #%L
 * mosaic-components-core
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

package eu.mosaic_cloud.components.core;


import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;


public interface ComponentCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference called (final ComponentController component, final ComponentCallRequest request);
	
	public abstract CallbackReference callReturned (final ComponentController component, final ComponentCallReply reply);
	
	public abstract CallbackReference casted (final ComponentController component, final ComponentCastRequest request);
	
	public abstract CallbackReference failed (final ComponentController component, final Throwable exception);
	
	public abstract CallbackReference initialized (final ComponentController component);
	
	public abstract CallbackReference registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok);
	
	public abstract CallbackReference terminated (final ComponentController component);
	
	public static interface Provider
	{
		public abstract ComponentCallbacks provide (final ComponentContext context);
	}
}
