/*
 * #%L
 * components-core
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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


import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;


public interface ComponentCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference called (final Component component, final ComponentCallRequest request);
	
	public abstract CallbackReference callReturned (final Component component, final ComponentCallReply reply);
	
	public abstract CallbackReference casted (final Component component, final ComponentCastRequest request);
	
	public abstract CallbackReference failed (final Component component, final Throwable exception);
	
	public abstract CallbackReference initialized (final Component component);
	
	public abstract CallbackReference registerReturn (final Component component, final ComponentCallReference reference, final boolean ok);
	
	public abstract CallbackReference terminated (final Component component);
}
