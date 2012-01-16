/*
 * #%L
 * mosaic-components-tools
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.components.tools;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;


public final class QueueingChannelCallbacks
		extends Object
		implements
			ChannelCallbacks,
			CallbackHandler<ChannelCallbacks>
{
	private QueueingChannelCallbacks (final Channel channel, final BlockingQueue<ChannelMessage> queue)
	{
		super ();
		Preconditions.checkNotNull (channel);
		Preconditions.checkNotNull (queue);
		this.channel = channel;
		this.queue = queue;
	}
	
	public final void assign ()
	{
		this.channel.assign (this);
	}
	
	@Override
	public final CallbackReference closed (final Channel channel, final ChannelFlow flow)
	{
		Preconditions.checkArgument (this.channel == channel);
		if (flow == ChannelFlow.Inbound)
			channel.terminate ();
		return (null);
	}
	
	@Override
	public final void deassigned (final ChannelCallbacks trigger, final ChannelCallbacks newCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public final CallbackReference failed (final Channel channel, final Throwable exception)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public final CallbackReference initialized (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public final void reassigned (final ChannelCallbacks trigger, final ChannelCallbacks oldCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public final CallbackReference received (final Channel channel, final ChannelMessage message)
	{
		Preconditions.checkArgument (this.channel == channel);
		Preconditions.checkNotNull (message);
		this.queue.add (message);
		return (null);
	}
	
	@Override
	public final void registered (final ChannelCallbacks trigger)
	{}
	
	@Override
	public final CallbackReference terminated (final Channel channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public final void unregistered (final ChannelCallbacks trigger)
	{}
	
	public final BlockingQueue<ChannelMessage> queue;
	private final Channel channel;
	
	public static final QueueingChannelCallbacks create (final Channel channel)
	{
		return (new QueueingChannelCallbacks (channel, new LinkedBlockingQueue<ChannelMessage> ()));
	}
	
	public static final QueueingChannelCallbacks create (final Channel channel, final BlockingQueue<ChannelMessage> queue)
	{
		return (new QueueingChannelCallbacks (channel, queue));
	}
}
