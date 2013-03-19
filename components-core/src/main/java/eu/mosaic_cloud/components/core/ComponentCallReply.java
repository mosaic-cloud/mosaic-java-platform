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


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallReply
		extends ComponentMessage
{
	private ComponentCallReply (final boolean ok, final Object outputsOrError, final ByteBuffer data, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (reference);
		this.ok = ok;
		this.outputsOrError = outputsOrError;
		this.data = data;
		this.reference = reference;
	}
	
	public static final ComponentCallReply create (final boolean ok, final Object outputsOrError, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallReply (ok, outputsOrError, data, reference));
	}
	
	public static final ComponentCallReply create (final boolean ok, final Object outputsOrError, final ComponentCallReference reference)
	{
		return (new ComponentCallReply (ok, outputsOrError, ByteBuffer.allocate (0), reference));
	}
	
	public final ByteBuffer data;
	public final boolean ok;
	public final Object outputsOrError;
	public final ComponentCallReference reference;
}
