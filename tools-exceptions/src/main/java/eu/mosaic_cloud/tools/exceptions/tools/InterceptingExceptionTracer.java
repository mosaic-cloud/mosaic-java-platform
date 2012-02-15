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

package eu.mosaic_cloud.tools.exceptions.tools;


import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;


public abstract class InterceptingExceptionTracer
		extends BaseExceptionTracer
{
	protected InterceptingExceptionTracer ()
	{
		super ();
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		try {
			this.trace_ (resolution, exception);
		} catch (final Throwable exception1) {
			// intentional
		}
		try {
			final ExceptionTracer delegate = this.getDelegate ();
			if (delegate != null)
				delegate.trace (resolution, exception);
		} catch (final Throwable exception1) {
			// intentional
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		try {
			this.trace_ (resolution, exception, message);
		} catch (final Throwable exception1) {
			// intentional
		}
		try {
			final ExceptionTracer delegate = this.getDelegate ();
			if (delegate != null)
				delegate.trace (resolution, exception, message);
		} catch (final Throwable exception1) {
			// intentional
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		try {
			this.trace_ (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {
			// intentional
		}
		try {
			final ExceptionTracer delegate = this.getDelegate ();
			if (delegate != null)
				delegate.trace (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {
			// intentional
		}
	}
	
	protected abstract ExceptionTracer getDelegate ();
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception);
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message);
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens);
}
