/*
 * #%L
 * mosaic-tools-exceptions
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

package eu.mosaic_cloud.tools.exceptions.core;


import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.InterceptingExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class FallbackExceptionTracer
			extends InterceptingExceptionTracer
{
	private FallbackExceptionTracer () {
		super ();
		this.globalDelegate = Atomics.newReference (null);
		this.threadDelegate = new InheritableThreadLocal<ExceptionTracer> ();
	}
	
	public final ExceptionTracer resolveDelegate () {
		{
			final ExceptionTracer delegate = this.globalDelegate.get ();
			if (delegate == this)
				return (null);
			if (delegate != null)
				return (delegate);
		}
		{
			final ExceptionTracer delegate = this.threadDelegate.get ();
			if (delegate == this)
				return (null);
			if (delegate != null)
				return (delegate);
		}
		return (AbortingExceptionTracer.defaultInstance);
	}
	
	public final void setGlobalTracer (final ExceptionTracer tracer) {
		Preconditions.checkNotNull (tracer);
		Preconditions.checkArgument (tracer != this);
		this.globalDelegate.set (tracer);
	}
	
	public final void setThreadTracer (final ExceptionTracer tracer) {
		Preconditions.checkNotNull (tracer);
		Preconditions.checkArgument (tracer != this);
		this.threadDelegate.set (tracer);
	}
	
	@Override
	protected final ExceptionTracer getDelegate () {
		return (this.resolveDelegate ());
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception) {
		// NOTE: intentional
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message) {
		// NOTE: intentional
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens) {
		// NOTE: intentional
	}
	
	private final AtomicReference<ExceptionTracer> globalDelegate;
	private final InheritableThreadLocal<ExceptionTracer> threadDelegate;
	public static final FallbackExceptionTracer defaultInstance = new FallbackExceptionTracer ();
}
