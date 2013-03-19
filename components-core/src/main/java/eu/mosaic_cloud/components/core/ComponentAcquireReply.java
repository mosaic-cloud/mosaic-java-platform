/*
 * #%L
 * mosaic-components-core
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

package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;


public final class ComponentAcquireReply
		extends ComponentMessage
{
	private ComponentAcquireReply (final ComponentResourceDescriptor descriptor, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkNotNull (reference);
		this.ok = true;
		this.descriptor = descriptor;
		this.reference = reference;
		this.error = null;
	}
	
	private ComponentAcquireReply (final Object error, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (error);
		Preconditions.checkNotNull (reference);
		this.ok = false;
		this.error = error;
		this.reference = reference;
		this.descriptor = null;
	}
	
	public static final ComponentAcquireReply create (final ComponentResourceDescriptor descriptor, final ComponentCallReference reference)
	{
		return (new ComponentAcquireReply (descriptor, reference));
	}
	
	public static final ComponentAcquireReply create (final Object error, final ComponentCallReference reference)
	{
		return (new ComponentAcquireReply (error, reference));
	}
	
	public final ComponentResourceDescriptor descriptor;
	public final Object error;
	public final boolean ok;
	public final ComponentCallReference reference;
}
