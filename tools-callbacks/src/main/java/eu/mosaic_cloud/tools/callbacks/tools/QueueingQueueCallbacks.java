/*
 * #%L
 * mosaic-tools-callbacks
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

package eu.mosaic_cloud.tools.callbacks.tools;


import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public final class QueueingQueueCallbacks<_Element_ extends Object>
		extends Object
		implements
			QueueCallbacks<_Element_>,
			CallbackHandler<QueueCallbacks<_Element_>>
{
	private QueueingQueueCallbacks (final BlockingQueue<_Element_> queue, final long waitTimeout)
	{
		super ();
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		this.queue = queue;
		this.waitTimeout = waitTimeout;
	}
	
	@Override
	public final void deassigned (final QueueCallbacks<_Element_> trigger, final QueueCallbacks<_Element_> newCallbacks)
	{}
	
	@Override
	public final CallbackReference enqueue (final _Element_ element)
	{
		if (!Threading.offer (this.queue, element, this.waitTimeout))
			throw (new BufferOverflowException ());
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
	private final long waitTimeout;
	
	public static final <_Element_ extends Object> QueueingQueueCallbacks<_Element_> create ()
	{
		return (new QueueingQueueCallbacks<_Element_> (new LinkedBlockingQueue<_Element_> (), 0));
	}
	
	public static final <_Element_ extends Object> QueueingQueueCallbacks<_Element_> create (final BlockingQueue<_Element_> queue, final long waitTimeout)
	{
		return (new QueueingQueueCallbacks<_Element_> (queue, waitTimeout));
	}
}
