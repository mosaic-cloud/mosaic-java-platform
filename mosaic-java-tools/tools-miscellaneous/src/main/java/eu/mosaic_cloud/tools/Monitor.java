
package eu.mosaic_cloud.tools;


import com.google.common.base.Preconditions;


public final class Monitor
		extends Object
{
	private Monitor ()
	{
		super ();
	}
	
	public static final Monitor create (final Object owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Monitor ());
	}
}
