/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.tools.configurations.core;


import java.util.IdentityHashMap;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;


public final class ConfigurationIdentifier
{
	private ConfigurationIdentifier (final Type type, final String path) {
		super ();
		Preconditions.checkNotNull (type);
		Preconditions.checkArgument (((type == Type.Abstract) && (path == null)) || (path != null));
		this.type = type;
		this.path = path;
	}
	
	@Override
	public final boolean equals (final Object other) {
		return (this == other);
	}
	
	@Override
	public final int hashCode () {
		return (System.identityHashCode (this));
	}
	
	public final ConfigurationIdentifier resolve (final ConfigurationIdentifier relative) {
		return (ConfigurationIdentifier.resolve (this, relative));
	}
	
	public final ConfigurationIdentifier resolve (final String relative) {
		return ConfigurationIdentifier.resolve (this, relative);
	}
	
	@Override
	public final String toString () {
		return ((this.path != null) ? this.path : "<abstract>");
	}
	
	public final String path;
	public final Type type;
	
	public static final ConfigurationIdentifier generateAbstract () {
		return (new ConfigurationIdentifier (Type.Abstract, null));
	}
	
	public static final ConfigurationIdentifier resolve (final ConfigurationIdentifier anchor, final ConfigurationIdentifier relative) {
		Preconditions.checkNotNull (anchor);
		Preconditions.checkNotNull (relative);
		Preconditions.checkArgument (relative.type == Type.Relative);
		return (ConfigurationIdentifier.resolve (anchor, relative.path));
	}
	
	public static final ConfigurationIdentifier resolve (final ConfigurationIdentifier anchor, final String relative) {
		return (ConfigurationIdentifier.resolve (anchor, relative, null));
	}
	
	public static final ConfigurationIdentifier resolveAbsolute (final String specification) {
		return ConfigurationIdentifier.resolve (null, specification, Type.Absolute);
	}
	
	public static final ConfigurationIdentifier resolveRelative (final String specification) {
		return ConfigurationIdentifier.resolve (null, specification, Type.Relative);
	}
	
	private static final ConfigurationIdentifier resolve (final ConfigurationIdentifier anchor, final String relative, final Type expectedType) {
		Preconditions.checkNotNull (relative);
		Preconditions.checkArgument (ConfigurationIdentifier.specificationPattern.matcher (relative).matches ());
		Preconditions.checkArgument ((anchor == null) || (anchor.type != Type.Abstract));
		final boolean anchorAbsolute = (anchor != null) && (anchor.type == Type.Absolute);
		final boolean relativeAbsolute = (relative.charAt (0) == '/');
		if (anchor != null)
			Preconditions.checkArgument (!relativeAbsolute);
		if (expectedType != null)
			switch (expectedType) {
				case Absolute :
					if (anchor != null)
						Preconditions.checkArgument (anchorAbsolute);
					else
						Preconditions.checkArgument (relativeAbsolute);
					break;
				case Relative :
					if (anchor != null)
						Preconditions.checkArgument (!anchorAbsolute);
					Preconditions.checkArgument (!relativeAbsolute);
					break;
				default :
					throw (new Error ());
			}
		final String path;
		if (anchor != null)
			if (anchor != ConfigurationIdentifier.root)
				path = (anchor.path + "/" + relative).intern ();
			else
				path = ("/" + relative).intern ();
		else
			path = relative.intern ();
		final boolean absolute = anchorAbsolute || relativeAbsolute;
		final ConfigurationIdentifier identifier;
		synchronized (ConfigurationIdentifier.identifiers) {
			if (ConfigurationIdentifier.identifiers.containsKey (path))
				identifier = ConfigurationIdentifier.identifiers.get (path);
			else
				identifier = new ConfigurationIdentifier (absolute ? Type.Absolute : Type.Relative, path);
			ConfigurationIdentifier.identifiers.put (path, identifier);
		}
		return (identifier);
	}
	
	static {
		identifiers = new IdentityHashMap<String, ConfigurationIdentifier> ();
		specificationPattern = Pattern.compile ("^/?([a-z]([a-z0-9_.]*[a-z0-9])?/)*([a-z]([a-z0-9_.]*[a-z0-9])?)$", Pattern.DOTALL);
		root = new ConfigurationIdentifier (Type.Absolute, "/");
	}
	public static final ConfigurationIdentifier root;
	private static final IdentityHashMap<String, ConfigurationIdentifier> identifiers;
	private static final Pattern specificationPattern;
	
	public enum Type
	{
		Absolute,
		Abstract,
		Relative;
	}
}
