/*
 * #%L
 * mosaic-tools-exceptions
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

package eu.mosaic_cloud.tools.exceptions.core;


import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.InterceptingExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public interface ExceptionTracer
{
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String message);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens);
	
	public static final DefaultExceptionTracer defaultInstance = new DefaultExceptionTracer ();
	
	public static final class DefaultExceptionTracer
			extends InterceptingExceptionTracer
	{
		DefaultExceptionTracer ()
		{
			super ();
			this.delegate = Atomics.newReference ((ExceptionTracer) AbortingExceptionTracer.defaultInstance);
		}
		
		public final void setTracer (final ExceptionTracer tracer)
		{
			Preconditions.checkNotNull (tracer);
			this.delegate.set (tracer);
		}
		
		@Override
		protected final ExceptionTracer getDelegate ()
		{
			return (this.delegate.get ());
		}
		
		@Override
		protected final void trace_ (final ExceptionResolution resolution, final Throwable exception)
		{}
		
		@Override
		protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
		{}
		
		@Override
		protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
		{}
		
		private final AtomicReference<ExceptionTracer> delegate;
	}
}
