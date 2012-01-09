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


public interface Component
{
	public abstract void assign (final ComponentCallbacks callbacks);
	
	public abstract void call (final ComponentIdentifier component, final ComponentCallRequest request);
	
	public abstract void cast (final ComponentIdentifier component, final ComponentCastRequest request);
	
	public abstract void register (final ComponentIdentifier group, final ComponentCallReference reference);
	
	public abstract void reply (final ComponentCallReply reply);
	
	public abstract void terminate ();
}
