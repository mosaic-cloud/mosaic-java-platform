
package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;


public abstract class ComponentResourceDescriptor
		extends Object
{
	ComponentResourceDescriptor (final String identifier)
	{
		super ();
		Preconditions.checkNotNull (identifier);
		this.identifier = identifier;
	}
	
	public final String identifier;
}
