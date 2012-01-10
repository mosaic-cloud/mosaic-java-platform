/*
 * #%L
 * mosaic-components-core
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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


public interface ChannelCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference closed (final Channel channel, final ChannelFlow flow);
	
	public abstract CallbackReference failed (final Channel channel, final Throwable exception);
	
	public abstract CallbackReference initialized (final Channel channel);
	
	public abstract CallbackReference received (final Channel channel, final ChannelMessage message);
	
	public abstract CallbackReference terminated (final Channel channel);
}
