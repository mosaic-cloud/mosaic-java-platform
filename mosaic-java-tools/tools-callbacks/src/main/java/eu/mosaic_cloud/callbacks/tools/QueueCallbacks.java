
package eu.mosaic_cloud.callbacks.tools;


import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;


public interface QueueCallbacks<_Element_ extends Object>
		extends
			Callbacks
{
	public abstract CallbackReference enqueue (final _Element_ value);
}
