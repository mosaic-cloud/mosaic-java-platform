
package eu.mosaic_cloud.callbacks.core;


public interface CallbackReactor
{
	public abstract <_Callbacks_ extends Callbacks> CallbackReference assign (final _Callbacks_ trigger, final _Callbacks_ delegate);
	
	public abstract <_Callbacks_ extends Callbacks> _Callbacks_ register (final Class<_Callbacks_> specification, final _Callbacks_ delegate);
	
	public abstract CallbackFuture resolve (final CallbackReference reference);
	
	public abstract void terminate ();
	
	public abstract <_Callbacks_ extends Callbacks> CallbackReference unregister (final _Callbacks_ trigger);
}
