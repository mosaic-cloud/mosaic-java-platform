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

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public final class QueueingComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler<ComponentCallbacks>
{
	private QueueingComponentCallbacks (final Component component, final BlockingQueue<ComponentMessage> queue, final long waitTimeout)
	{
		super ();
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		this.component = component;
		this.queue = queue;
		this.waitTimeout = waitTimeout;
	}
	
	public final void assign ()
	{
		this.component.assign (this);
	}
	
	@Override
	public final CallbackReference called (final Component component, final ComponentCallRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		if (!Threading.offer (this.queue, request, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final CallbackReference callReturned (final Component component, final ComponentCallReply reply)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (reply);
		if (!Threading.offer (this.queue, reply, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final CallbackReference casted (final Component component, final ComponentCastRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		if (!Threading.offer (this.queue, request, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final void deassigned (final ComponentCallbacks trigger, final ComponentCallbacks newCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public final CallbackReference failed (final Component component, final Throwable exception)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public final CallbackReference initialized (final Component component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public final void reassigned (final ComponentCallbacks trigger, final ComponentCallbacks oldCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public final void registered (final ComponentCallbacks trigger)
	{}
	
	@Override
	public final CallbackReference registerReturn (final Component component, final ComponentCallReference reference, final boolean ok)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackReference terminated (final Component component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public final void unregistered (final ComponentCallbacks trigger)
	{}
	
	public final BlockingQueue<ComponentMessage> queue;
	private final Component component;
	private final long waitTimeout;
	
	public static final QueueingComponentCallbacks create (final Component component)
	{
		return (new QueueingComponentCallbacks (component, new LinkedBlockingQueue<ComponentMessage> (), 0));
	}
	
	public static final QueueingComponentCallbacks create (final Component component, final BlockingQueue<ComponentMessage> queue, final long waitTimeout)
	{
		return (new QueueingComponentCallbacks (component, queue, waitTimeout));
	}
}
