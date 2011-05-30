
package eu.mosaic_cloud.components.tools;


import java.util.concurrent.BlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelMessage;


public class QueueingChannelCallbacks
		extends Object
		implements
			ChannelCallbacks,
			CallbackHandler<ChannelCallbacks>
{
	public QueueingChannelCallbacks (final Channel channel, final BlockingQueue<ChannelMessage> queue)
	{
		super ();
		Preconditions.checkNotNull (channel);
		Preconditions.checkNotNull (queue);
		this.channel = channel;
		this.queue = queue;
	}
	
	@Override
	public CallbackReference closed (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public void deassigned (final ChannelCallbacks trigger, final ChannelCallbacks newCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public CallbackReference failed (final Channel channel, final Throwable exception)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	public void initialize ()
	{
		this.channel.assign (this);
	}
	
	@Override
	public CallbackReference opened (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public void reassigned (final ChannelCallbacks trigger, final ChannelCallbacks oldCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public CallbackReference received (final Channel channel, final ChannelMessage message)
	{
		Preconditions.checkArgument (this.channel == channel);
		Preconditions.checkNotNull (message);
		this.queue.add (message);
		return (null);
	}
	
	@Override
	public void registered (final ChannelCallbacks trigger)
	{}
	
	@Override
	public void unregistered (final ChannelCallbacks trigger)
	{}
	
	protected final Channel channel;
	protected final BlockingQueue<ChannelMessage> queue;
}
