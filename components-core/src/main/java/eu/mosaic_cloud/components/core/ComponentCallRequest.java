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


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallRequest
		extends ComponentMessage
{
	private ComponentCallRequest (final String operation, final Object inputs, final ByteBuffer data, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (operation);
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (reference);
		this.operation = operation;
		this.inputs = inputs;
		this.data = data;
		this.reference = reference;
	}
	
	public final ByteBuffer data;
	public final Object inputs;
	public final String operation;
	public final ComponentCallReference reference;
	
	public static final ComponentCallRequest create (final String operation, final Object inputs, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallRequest (operation, inputs, data, reference));
	}
	
	public static final ComponentCallRequest create (final String operation, final Object inputs, final ComponentCallReference reference)
	{
		return (new ComponentCallRequest (operation, inputs, ByteBuffer.allocate (0), reference));
	}
}
