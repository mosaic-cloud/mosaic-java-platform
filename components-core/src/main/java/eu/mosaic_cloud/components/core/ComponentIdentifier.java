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


import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;


public final class ComponentIdentifier
		extends Object
{
	private ComponentIdentifier (final String string)
	{
		super ();
		this.string = string;
	}
	
	@Override
	public final String toString ()
	{
		return (this.string);
	}
	
	static {
		identifiers = HashBiMap.create ();
		stringPattern = Pattern.compile ("^[0-9a-f]{40}$");
		standalone = ComponentIdentifier.resolve ("ffffffffffffffffffffffffffffffffffffffff");
	}
	
	public static final ComponentIdentifier resolve (final String string)
	{
		Preconditions.checkNotNull (string);
		Preconditions.checkArgument (ComponentIdentifier.stringPattern.matcher (string).matches ());
		synchronized (ComponentIdentifier.identifiers) {
			final ComponentIdentifier existingIdentifier = ComponentIdentifier.identifiers.get (string);
			final ComponentIdentifier identifier;
			if (existingIdentifier != null)
				identifier = existingIdentifier;
			else {
				identifier = new ComponentIdentifier (string);
				ComponentIdentifier.identifiers.put (string, identifier);
			}
			return (identifier);
		}
	}
	
	public final String string;
	public static final ComponentIdentifier standalone;
	public static final Pattern stringPattern;
	private static final HashBiMap<String, ComponentIdentifier> identifiers;
}
