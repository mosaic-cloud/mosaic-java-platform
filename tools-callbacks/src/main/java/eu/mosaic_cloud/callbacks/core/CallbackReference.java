
package eu.mosaic_cloud.callbacks.core;


import com.google.common.base.Preconditions;


public final class CallbackReference
		extends Object
{
	private CallbackReference (final CallbackReactor reactor)
	{
		super ();
		this.reactor = reactor;
	}
	
	public final CallbackReactor reactor;
	
	public static final CallbackReference create (final CallbackReactor reactor)
	{
		Preconditions.checkNotNull (reactor);
		return (new CallbackReference (reactor));
	}
}
