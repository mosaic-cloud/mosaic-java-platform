/*
 * #%L
 * mosaic-tools-callbacks
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
