
package eu.mosaic_cloud.components.core;

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
	
	public final String string;
	
	public static final ComponentIdentifier resolve (final String string) {
		Preconditions.checkNotNull (string);
		synchronized (identifiers) {
			final ComponentIdentifier existingIdentifier = identifiers.get (string);
			final ComponentIdentifier identifier;
			if (existingIdentifier != null)
				identifier = existingIdentifier;
			else {
				identifier = new ComponentIdentifier (string);
				identifiers.put (string, identifier);
			}
			return (identifier);
		}
	}
	
	private static final HashBiMap<String, ComponentIdentifier> identifiers = HashBiMap.create ();
}
