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

package eu.mosaic_cloud.tools.callbacks.core;


import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.Joinable;

import com.google.common.base.Preconditions;


public final class CallbackIsolate
		extends Object
		implements
			Joinable
{
	// FIXME: See the `FIXME` notice at the top of the `CallbackCompletion` class.
	private CallbackIsolate (final CallbackIsolateBackend backend)
	{
		super ();
		Preconditions.checkNotNull (backend);
		this.backend = backend;
	}
	
	@Override
	public final boolean await ()
	{
		return (this.await (-1));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		try {
			return (this.backend.awaitIsolate (this, timeout));
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the `CallbackCompletion` class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			return (false);
		}
	}
	
	public final CallbackCompletion<Void> destroy ()
	{
		try {
			final CallbackCompletion<Void> completion = this.backend.destroyIsolate (this);
			if (completion == null)
				return (CallbackCompletion.createFailure (new IllegalStateException ()));
			return (completion);
		} catch (final Throwable exception) {
			return (CallbackCompletion.createFailure (exception));
		}
	}
	
	public final CallbackCompletion<Void> enqueue (final Runnable runnable)
	{
		Preconditions.checkNotNull (runnable);
		try {
			return (this.backend.enqueueOnIsolate (this, runnable));
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the `CallbackCompletion` class.
			FallbackExceptionTracer.defaultInstance.traceDeferredException (exception);
			return (CallbackCompletion.createFailure (exception));
		}
	}
	
	public final CallbackReactor getReactor ()
	{
		try {
			return (this.backend.getReactor ());
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the `CallbackCompletion` class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			return (null);
		}
	}
	
	public static final CallbackIsolate create (final CallbackIsolateBackend backend)
	{
		return (new CallbackIsolate (backend));
	}
	
	private final CallbackIsolateBackend backend;
}
