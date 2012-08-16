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

import eu.mosaic_cloud.components.core.ComponentAcquireReply;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import com.google.common.base.Preconditions;


public final class QueueingComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler
{
	private QueueingComponentCallbacks (final ComponentController component, final BlockingQueue<ComponentMessage> queue, final long waitTimeout, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		Preconditions.checkNotNull (exceptions);
		this.component = component;
		this.queue = queue;
		this.waitTimeout = waitTimeout;
		this.exceptions = exceptions;
	}
	
	@Override
	public final CallbackCompletion<Void> acquireReturned (final ComponentController component, final ComponentAcquireReply reply)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		if (!Threading.offer (this.queue, request, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (reply);
		if (!Threading.offer (this.queue, reply, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		if (!Threading.offer (this.queue, request, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
	{
		Preconditions.checkArgument (this.component == component);
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
		return (null);
	}
	
	@Override
	public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
	{
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
	}
	
	@Override
	public final CallbackCompletion<Void> initialized (final ComponentController component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
	{}
	
	@Override
	public final CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackCompletion<Void> terminated (final ComponentController component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks proxy)
	{}
	
	public static final QueueingComponentCallbacks create (final ComponentController component, final BlockingQueue<ComponentMessage> queue, final long waitTimeout, final ExceptionTracer exceptions)
	{
		return (new QueueingComponentCallbacks (component, queue, waitTimeout, exceptions));
	}
	
	public static final QueueingComponentCallbacks create (final ComponentController component, final ExceptionTracer exceptions)
	{
		return (new QueueingComponentCallbacks (component, new LinkedBlockingQueue<ComponentMessage> (), 0, exceptions));
	}
	
	public final BlockingQueue<ComponentMessage> queue;
	private final ComponentController component;
	private final ExceptionTracer exceptions;
	private final long waitTimeout;
}
