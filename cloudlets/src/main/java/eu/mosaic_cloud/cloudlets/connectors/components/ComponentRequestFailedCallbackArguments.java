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


public final class ComponentRequestFailedCallbackArguments<TExtra>
		extends CallbackArguments
{
	public ComponentRequestFailedCallbackArguments (final ICloudletController<?> cloudlet, final Throwable exception, final TExtra extra)
	{
		super (cloudlet);
		this.exception = exception;
		this.extra = extra;
	}
	
	public Throwable getException ()
	{
		return (this.exception);
	}
	
	public TExtra getExtra ()
	{
		return (this.extra);
	}
	
	private final Throwable exception;
	private final TExtra extra;
}
