
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
}
