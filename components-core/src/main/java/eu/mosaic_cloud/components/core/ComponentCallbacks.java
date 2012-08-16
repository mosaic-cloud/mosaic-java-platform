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


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;


public interface ComponentCallbacks
		extends
			Callbacks
{
	public abstract CallbackCompletion<Void> acquireReturned (final ComponentController component, final ComponentAcquireReply reply);
	
	public abstract CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request);
	
	public abstract CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply);
	
	public abstract CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request);
	
	public abstract CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception);
	
	public abstract CallbackCompletion<Void> initialized (final ComponentController component);
	
	public abstract CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok);
	
	public abstract CallbackCompletion<Void> terminated (final ComponentController component);
}
