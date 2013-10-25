
package eu.mosaic_cloud.platform.v2.connectors.core;


import java.util.IdentityHashMap;

import com.google.common.base.Preconditions;


public final class ConnectorVariant
{
	private ConnectorVariant (final String specifier) {
		super ();
		Preconditions.checkNotNull (specifier);
		this.specifier = specifier;
	}
	
	public final String specifier;
	
	public static final ConnectorVariant resolve (final String specifier_) {
		Preconditions.checkNotNull (specifier_);
		final String specifier = specifier_.intern ();
		final ConnectorVariant variant;
		synchronized (ConnectorVariant.variants) {
			final ConnectorVariant existingVariant = ConnectorVariant.variants.get (specifier);
			if (existingVariant != null)
				variant = existingVariant;
			else
				variant = new ConnectorVariant (specifier);
			ConnectorVariant.variants.put (specifier, variant);
		}
		return (variant);
	}
	
	static {
		variants = new IdentityHashMap<String, ConnectorVariant> ();
		fallback = ConnectorVariant.resolve ("__fallback__");
	}
	public static final ConnectorVariant fallback;
	private static final IdentityHashMap<String, ConnectorVariant> variants;
}
