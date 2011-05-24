
package eu.mosaic_cloud.components.tools;


import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelMessage;


public final class QueueingChannelCallbacks
		extends Object
		implements
			ChannelCallbacks,
			BlockingQueue<ChannelMessage>
{
	public QueueingChannelCallbacks (final Channel channel)
	{
		super ();
		Preconditions.checkNotNull (channel);
		this.channel = channel;
		this.queue = new LinkedBlockingQueue<ChannelMessage> ();
		this.channel.setCallbacks (this);
	}
	
	@Override
	public boolean add (final ChannelMessage message)
	{
		this.channel.send (message);
		return (true);
	}
	
	@Override
	public boolean addAll (final Collection<? extends ChannelMessage> messages)
	{
		for (final ChannelMessage message : messages)
			this.channel.send (message);
		return (true);
	}
	
	@Override
	public void clear ()
	{
		this.queue.clear ();
	}
	
	@Override
	public void closed (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
	}
	
	@Override
	public boolean contains (final Object o)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public boolean containsAll (final Collection<?> objects)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public int drainTo (final Collection<? super ChannelMessage> sink)
	{
		return (this.queue.drainTo (sink));
	}
	
	@Override
	public int drainTo (final Collection<? super ChannelMessage> sink, final int elements)
	{
		return (this.queue.drainTo (sink, elements));
	}
	
	@Override
	public ChannelMessage element ()
	{
		return (this.queue.element ());
	}
	
	@Override
	public void failed (final Channel channel, final Throwable exception)
	{
		Preconditions.checkArgument (this.channel == channel);
	}
	
	public final Channel getChannel ()
	{
		return (this.channel);
	}
	
	@Override
	public boolean isEmpty ()
	{
		return (this.queue.isEmpty ());
	}
	
	@Override
	public Iterator<ChannelMessage> iterator ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public boolean offer (final ChannelMessage message)
	{
		this.channel.send (message);
		return (true);
	}
	
	@Override
	public boolean offer (final ChannelMessage message, final long timeout, final TimeUnit timeoutUnit)
	{
		this.channel.send (message);
		return (true);
	}
	
	@Override
	public void opened (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
	}
	
	@Override
	public ChannelMessage peek ()
	{
		return (this.queue.peek ());
	}
	
	@Override
	public ChannelMessage poll ()
	{
		return (this.queue.poll ());
	}
	
	@Override
	public ChannelMessage poll (final long timeout, final TimeUnit timeoutUnit)
			throws InterruptedException
	{
		return (this.queue.poll (timeout, timeoutUnit));
	}
	
	@Override
	public void put (final ChannelMessage message)
	{
		this.channel.send (message);
	}
	
	@Override
	public void received (final Channel channel, final ChannelMessage message)
	{
		Preconditions.checkArgument (this.channel == channel);
		Preconditions.checkNotNull (message);
		this.queue.add (message);
	}
	
	@Override
	public int remainingCapacity ()
	{
		return (this.queue.remainingCapacity ());
	}
	
	@Override
	public ChannelMessage remove ()
	{
		return (this.queue.element ());
	}
	
	@Override
	public boolean remove (final Object object)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public boolean removeAll (final Collection<?> objects)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public boolean retainAll (final Collection<?> objects)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public int size ()
	{
		return (this.queue.size ());
	}
	
	@Override
	public ChannelMessage take ()
			throws InterruptedException
	{
		return (this.queue.take ());
	}
	
	@Override
	public Object[] toArray ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public <T> T[] toArray (final T[] template)
	{
		throw (new UnsupportedOperationException ());
	}
	
	private final Channel channel;
	private final LinkedBlockingQueue<ChannelMessage> queue;
}
