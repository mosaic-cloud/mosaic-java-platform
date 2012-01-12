/*
 * #%L
 * mosaic-interoperability-tools
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

package eu.mosaic_cloud.interoperability.tools;


import java.util.UUID;

import com.google.common.base.Preconditions;


public final class Identifiers
		extends Object
{
	private Identifiers ()
	{
		throw (new IllegalAccessError ());
	}
	
	public static final String generate (final Enum<?> object)
	{
		Preconditions.checkNotNull (object);
		return (UUID.nameUUIDFromBytes ((object.getClass ().getName () + ":" + object.name ()).getBytes ()).toString ());
	}
	
	public static final String generateName (final Enum<?> object)
	{
		Preconditions.checkNotNull (object);
		return (object.getClass ().getName () + ":" + object.name ());
	}
}
