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

package eu.mosaic_cloud.exceptions.tools;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public final class FanoutExceptionTracer
		extends InterceptingExceptionTracer
{
	private FanoutExceptionTracer (final ExceptionTracer[] delegates)
	{
		super (NullExceptionTracer.defaultInstance);
		Preconditions.checkNotNull (delegates);
		this.delegates = delegates;
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception)
	{
		for (final ExceptionTracer delegate : this.delegates)
			try {
				delegate.trace (resolution, exception);
			} catch (final Throwable exception1) {
				// intentional
			}
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		for (final ExceptionTracer delegate : this.delegates)
			try {
				delegate.trace (resolution, exception, message);
			} catch (final Throwable exception1) {
				// intentional
			}
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		for (final ExceptionTracer delegate : this.delegates)
			try {
				delegate.trace (resolution, exception, format, tokens);
			} catch (final Throwable exception1) {
				// intentional
			}
	}
	
	private final ExceptionTracer[] delegates;
	
	public static final FanoutExceptionTracer create (final ExceptionTracer ... delegates)
	{
		return (new FanoutExceptionTracer (delegates));
	}
}
