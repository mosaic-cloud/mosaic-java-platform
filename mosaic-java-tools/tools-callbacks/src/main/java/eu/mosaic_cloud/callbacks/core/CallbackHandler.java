
package eu.mosaic_cloud.callbacks.core;


public interface CallbackHandler<_Callbacks_ extends Callbacks>
{
	public abstract void deassigned (final _Callbacks_ trigger, final _Callbacks_ newCallbacks);
	
	public abstract void reassigned (final _Callbacks_ trigger, final _Callbacks_ oldCallbacks);
	
	public abstract void registered (final _Callbacks_ trigger);
	
	public abstract void unregistered (final _Callbacks_ trigger);
}
