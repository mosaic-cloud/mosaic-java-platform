/*
 * #%L
 * mosaic-examples-simple-components
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.examples.components.simple;


import eu.mosaic_cloud.components.core.ComponentAcquireReply;
import eu.mosaic_cloud.components.core.ComponentAcquireRequest;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentTcpSocketResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;


public final class AcquireTestComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler
{
	public AcquireTestComponentCallbacks (final ComponentEnvironment context)
	{
		super ();
		this.transcript = Transcript.create (this, true);
		this.component = null;
		this.pending = null;
	}
	
	@Override
	public final CallbackCompletion<Void> acquireReturned (final ComponentController component, final ComponentAcquireReply reply)
	{
		Preconditions.checkArgument (reply.reference == this.pending);
		if (reply.ok) {
			this.transcript.traceInformation ("acquire succeeded `%s`", reply.descriptor);
		} else {
			this.transcript.traceInformation ("acquire failed `%s`", reply.error);
		}
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
	{
		Preconditions.checkState (this.component == component);
		this.component = component;
		return (null);
	}
	
	@Override
	public final void failedCallbacks (final Callbacks trigger, final Throwable exception)
	{
		this.failed (this.component, exception);
	}
	
	@Override
	public final CallbackCompletion<Void> initialized (final ComponentController component)
	{
		Preconditions.checkState (this.component == null);
		this.component = component;
		this.pending = ComponentCallReference.create ();
		this.component.acquire (ComponentAcquireRequest.create (ComponentTcpSocketResourceSpecification.create ("socket"), this.pending));
		return (null);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks trigger, final CallbackIsolate isolate)
	{
		Preconditions.checkState (this.component == null);
	}
	
	@Override
	public CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackCompletion<Void> terminated (final ComponentController component)
	{
		Preconditions.checkState (this.component == component);
		this.component = null;
		return (null);
	}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks trigger)
	{
		this.component = null;
	}
	
	private ComponentController component;
	private ComponentCallReference pending;
	private final Transcript transcript;
}
