
package eu.mosaic_cloud.components.core;


public final class ComponentCallReference
		extends Object
{
	private ComponentCallReference ()
	{
		super ();
	}
	
	public static final ComponentCallReference create ()
	{
		return (new ComponentCallReference ());
	}
}
