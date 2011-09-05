
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
	
	public final String string;
	
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
	
	public static final Pattern stringPattern = Pattern.compile ("^[0-9a-f]{40}$");
	private static final HashBiMap<String, ComponentIdentifier> identifiers = HashBiMap.create ();
}
