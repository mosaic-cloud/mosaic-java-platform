
package eu.mosaic_cloud.callbacks.tools;


import java.util.concurrent.BlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;


public final class QueueingQueueCallbacks<_Element_ extends Object>
		extends Object
		implements
			QueueCallbacks<_Element_>,
			CallbackHandler<QueueCallbacks<_Element_>>
{
	public QueueingQueueCallbacks (final BlockingQueue<_Element_> queue)
	{
		super ();
		Preconditions.checkNotNull (queue);
		this.queue = queue;
	}
	
	@Override
	public void deassigned (final QueueCallbacks<_Element_> trigger, final QueueCallbacks<_Element_> newCallbacks)
	{}
	
	@Override
	public final CallbackReference enqueue (final _Element_ element)
	{
		this.queue.add (element);
		return (null);
	}
	
	@Override
	public void reassigned (final QueueCallbacks<_Element_> trigger, final QueueCallbacks<_Element_> oldCallbacks)
	{}
	
	@Override
	public void registered (final QueueCallbacks<_Element_> trigger)
	{}
	
	@Override
	public void unregistered (final QueueCallbacks<_Element_> trigger)
	{}
	
	private final BlockingQueue<_Element_> queue;
}
