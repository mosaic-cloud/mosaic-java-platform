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


import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;


public abstract class CaughtException
		extends Throwable
{
	CaughtException (final ExceptionResolution resolution, final Throwable caught)
	{
		super (caught);
		Preconditions.checkNotNull (caught);
		this.caught = caught;
		this.resolution = resolution;
		this.messageFormat = null;
		this.messageArguments = null;
	}
	
	CaughtException (final ExceptionResolution resolution, final Throwable caught, final String message)
	{
		super (message, caught);
		Preconditions.checkNotNull (caught);
		this.caught = caught;
		this.resolution = resolution;
		this.messageFormat = null;
		this.messageArguments = null;
	}
	
	CaughtException (final ExceptionResolution resolution, final Throwable caught, final String messageFormat, final Object ... messageArguments)
	{
		super (String.format (messageFormat, messageArguments), caught);
		Preconditions.checkNotNull (caught);
		this.caught = caught;
		this.resolution = resolution;
		this.messageFormat = messageFormat;
		this.messageArguments = messageArguments;
	}
	
	public final Throwable getCaught ()
	{
		return (this.caught);
	}
	
	public final Object[] getMessageArguments ()
	{
		return (this.messageArguments);
	}
	
	public final String getMessageFormat ()
	{
		return (this.messageFormat);
	}
	
	public final ExceptionResolution getResolution ()
	{
		return (this.resolution);
	}
	
	public final void rethrow ()
	{
		Throwables.propagate (this.caught);
	}
	
	public final void trace (final ExceptionResolution resolution, final ExceptionTracer tracer)
	{
		tracer.trace (ExceptionResolution.Handled, this);
		final Throwable cause = this.getCause ();
		if (this.messageFormat != null) {
			final String message = this.getMessage ();
			if (message != null)
				tracer.trace (resolution, cause, message);
			else
				tracer.trace (resolution, cause);
		} else
			tracer.trace (resolution, cause, this.messageFormat, this.messageArguments);
	}
	
	public final void trace (final ExceptionTracer tracer)
	{
		this.trace (this.resolution, tracer);
	}
	
	public final Wrapper wrap ()
	{
		return (new Wrapper (this));
	}
	
	public static final CaughtException create (final ExceptionResolution resolution, final Throwable caught)
	{
		switch (resolution) {
			case Handled :
				return (new HandledException (caught));
			case Deferred :
				return (new DeferredException (caught));
			case Ignored :
				return (new IgnoredException (caught));
			default:
				throw (new AssertionError ());
		}
	}
	
	public static final CaughtException create (final ExceptionResolution resolution, final Throwable caught, final String message)
	{
		switch (resolution) {
			case Handled :
				return (new HandledException (caught, message));
			case Deferred :
				return (new DeferredException (caught, message));
			case Ignored :
				return (new IgnoredException (caught, message));
			default:
				throw (new AssertionError ());
		}
	}
	
	public static final CaughtException create (final ExceptionResolution resolution, final Throwable caught, final String messageFormat, final Object ... messageArguments)
	{
		switch (resolution) {
			case Handled :
				return (new HandledException (caught, messageFormat, messageArguments));
			case Deferred :
				return (new DeferredException (caught, messageFormat, messageArguments));
			case Ignored :
				return (new IgnoredException (caught, messageFormat, messageArguments));
			default:
				throw (new AssertionError ());
		}
	}
	
	public final Throwable caught;
	public final ExceptionResolution resolution;
	protected final Object[] messageArguments;
	protected final String messageFormat;
	private static final long serialVersionUID = 1L;
	
	public static final class Wrapper
			extends Error
	{
		Wrapper (final CaughtException exception)
		{
			super (exception);
			this.exception = exception;
		}
		
		public final void rethrow ()
		{
			this.exception.rethrow ();
		}
		
		public final void trace (final ExceptionResolution resolution, final ExceptionTracer tracer)
		{
			tracer.trace (ExceptionResolution.Handled, this);
			this.exception.trace (resolution, tracer);
		}
		
		public final void trace (final ExceptionTracer tracer)
		{
			tracer.trace (ExceptionResolution.Handled, this);
			this.exception.trace (tracer);
		}
		
		public final CaughtException exception;
		private static final long serialVersionUID = 1L;
	}
}
