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


import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelController;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import com.google.common.base.Preconditions;


public final class QueueingChannelCallbacks
		extends Object
		implements
			ChannelCallbacks,
			CallbackHandler
{
	private QueueingChannelCallbacks (final ChannelController channel, final BlockingQueue<ChannelMessage> queue, final long waitTimeout, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (channel);
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		Preconditions.checkNotNull (exceptions);
		this.channel = channel;
		this.queue = queue;
		this.waitTimeout = waitTimeout;
		this.exceptions = exceptions;
	}
	
	@Override
	public final CallbackCompletion<Void> closed (final ChannelController channel, final ChannelFlow flow)
	{
		Preconditions.checkArgument (this.channel == channel);
		if (flow == ChannelFlow.Inbound)
			this.channel.terminate ();
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final ChannelController channel, final Throwable exception)
	{
		Preconditions.checkArgument (this.channel == channel);
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
		return (null);
	}
	
	@Override
	public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
	{
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
	}
	
	@Override
	public final CallbackCompletion<Void> initialized (final ChannelController channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> received (final ChannelController channel, final ChannelMessage message)
	{
		Preconditions.checkArgument (this.channel == channel);
		Preconditions.checkNotNull (message);
		if (!Threading.offer (this.queue, message, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
	{}
	
	@Override
	public final CallbackCompletion<Void> terminated (final ChannelController channel)
	{
		Preconditions.checkArgument (this.channel == channel);
		return (null);
	}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks proxy)
	{}
	
	public final BlockingQueue<ChannelMessage> queue;
	private final ChannelController channel;
	private final ExceptionTracer exceptions;
	private final long waitTimeout;
	
	public static final QueueingChannelCallbacks create (final ChannelController channel, final BlockingQueue<ChannelMessage> queue, final long waitTimeout, final ExceptionTracer exceptions)
	{
		return (new QueueingChannelCallbacks (channel, queue, waitTimeout, exceptions));
	}
	
	public static final QueueingChannelCallbacks create (final ChannelController channel, final ExceptionTracer exceptions)
	{
		return (new QueueingChannelCallbacks (channel, new LinkedBlockingQueue<ChannelMessage> (), 0, exceptions));
	}
}
