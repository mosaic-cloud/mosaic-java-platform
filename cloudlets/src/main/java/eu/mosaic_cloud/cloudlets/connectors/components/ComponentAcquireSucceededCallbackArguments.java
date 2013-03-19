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

package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;


public class ComponentAcquireSucceededCallbackArguments<TExtra>
		extends CallbackArguments
{
	public ComponentAcquireSucceededCallbackArguments (final ICloudletController<?> cloudlet, final ComponentResourceDescriptor descriptor, final TExtra extra)
	{
		super (cloudlet);
		this.descriptor = descriptor;
		this.extra = extra;
	}
	
	public ComponentResourceDescriptor getDescriptor ()
	{
		return (this.descriptor);
	}
	
	public TExtra getExtra ()
	{
		return (this.extra);
	}
	
	private final ComponentResourceDescriptor descriptor;
	private final TExtra extra;
}
