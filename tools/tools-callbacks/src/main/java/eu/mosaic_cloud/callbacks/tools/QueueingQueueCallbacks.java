
package eu.mosaic_cloud.callbacks.tools;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;


public final class QueueingQueueCallbacks<_Element_ extends Object>
		extends Object
		implements
			QueueCallbacks<_Element_>,
			CallbackHandler<QueueCallbacks<_Element_>>
{
	private QueueingQueueCallbacks (final BlockingQueue<_Element_> queue)
	{
		super ();
		Preconditions.checkNotNull (queue);
		this.queue = queue;
	}
	
	@Override
	public final void deassigned (final QueueCallbacks<_Element_> trigger, final QueueCallbacks<_Element_> newCallbacks)
	{}
	
	@Override
	public final CallbackReference enqueue (final _Element_ element)
	{
		this.queue.add (element);
		return (null);
	}
	
	@Override
	public final void reassigned (final QueueCallbacks<_Element_> trigger, final QueueCallbacks<_Element_> oldCallbacks)
	{}
	
	@Override
	public final void registered (final QueueCallbacks<_Element_> trigger)
	{}
	
	@Override
	public final void unregistered (final QueueCallbacks<_Element_> trigger)
	{}
	
	public final BlockingQueue<_Element_> queue;
	
	public static final <_Element_ extends Object> QueueingQueueCallbacks<_Element_> create ()
	{
		return (new QueueingQueueCallbacks<_Element_> (new LinkedBlockingQueue<_Element_> ()));
	}
	
	public static final <_Element_ extends Object> QueueingQueueCallbacks<_Element_> create (final BlockingQueue<_Element_> queue)
	{
		return (new QueueingQueueCallbacks<_Element_> (queue));
	}
}
