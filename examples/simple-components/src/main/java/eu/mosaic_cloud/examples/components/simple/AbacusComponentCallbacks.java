/*
 * #%L
 * mosaic-examples-simple-components
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

package eu.mosaic_cloud.examples.components.simple;


import java.nio.ByteBuffer;
import java.util.List;

import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonMapper;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;


public final class AbacusComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler
{
	public AbacusComponentCallbacks (final ComponentEnvironment context)
	{
		super ();
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, context.exceptions);
		this.component = null;
		this.status = Status.Created;
	}
	
	@Override
	public final CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		Preconditions.checkArgument (request.data.remaining () == 0);
		if ("+".equals (request.operation)) {
			final List<?> operands;
			try {
				operands = DefaultJsonMapper.defaultInstance.decode (request.inputs, List.class);
				Preconditions.checkNotNull (operands);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
				return (null);
			}
			this.transcript.traceInformation ("called `%s` with `%s`", request.operation, Joiner.on ("`, `").join (operands));
			double outcome;
			if ("+".equals (request.operation)) {
				outcome = 0;
				try {
					for (final Object operand : operands)
						outcome += ((Number) operand).doubleValue ();
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
					return (null);
				}
			} else
				throw (new IllegalArgumentException ());
			final ComponentCallReply reply = ComponentCallReply.create (true, Double.valueOf (outcome), ByteBuffer.allocate (0), request.reference);
			component.callReturn (reply);
		}
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		this.exceptions.traceIgnoredException (exception);
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
		Preconditions.checkState (this.status == Status.Registered);
		Preconditions.checkState (this.component == null);
		this.component = component;
		this.status = Status.Initialized;
		return (null);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks trigger, final CallbackIsolate isolate)
	{
		Preconditions.checkState (this.status == Status.Created);
		Preconditions.checkState (this.component == null);
		this.status = Status.Registered;
	}
	
	@Override
	public CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackCompletion<Void> terminated (final ComponentController component)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		this.component = null;
		this.status = Status.Terminated;
		return (null);
	}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks trigger)
	{
		Preconditions.checkState ((this.status == Status.Registered) || (this.status == Status.Initialized) || (this.status == Status.Terminated));
		this.component = null;
		this.status = Status.Unregistered;
	}
	
	private ComponentController component;
	private final TranscriptExceptionTracer exceptions;
	private Status status;
	private final Transcript transcript;
	
	private static enum Status
	{
		Created (),
		Initialized (),
		Registered (),
		Terminated (),
		Unregistered ();
	}
}
