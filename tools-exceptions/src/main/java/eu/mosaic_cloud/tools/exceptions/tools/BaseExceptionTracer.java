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

package eu.mosaic_cloud.tools.exceptions.tools;


import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;


public abstract class BaseExceptionTracer
			extends Object
			implements
				ExceptionTracer
{
	protected BaseExceptionTracer () {
		super ();
		this.catcher = UncaughtExceptionHandler.create (this);
	}
	
	public final void traceCaughtException (final CaughtException exception) {
		exception.trace (this);
	}
	
	public final void traceDeferredException (final Throwable exception) {
		this.trace (ExceptionResolution.Deferred, exception);
	}
	
	public final void traceDeferredException (final Throwable exception, final String message) {
		this.trace (ExceptionResolution.Deferred, exception, message);
	}
	
	public final void traceDeferredException (final Throwable exception, final String format, final Object ... tokens) {
		this.trace (ExceptionResolution.Deferred, exception, format, tokens);
	}
	
	public final void traceHandledException (final Throwable exception) {
		this.trace (ExceptionResolution.Handled, exception);
	}
	
	public final void traceHandledException (final Throwable exception, final String message) {
		this.trace (ExceptionResolution.Handled, exception, message);
	}
	
	public final void traceHandledException (final Throwable exception, final String format, final Object ... tokens) {
		this.trace (ExceptionResolution.Handled, exception, format, tokens);
	}
	
	public final void traceIgnoredException (final Throwable exception) {
		this.trace (ExceptionResolution.Ignored, exception);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String message) {
		this.trace (ExceptionResolution.Ignored, exception, message);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String format, final Object ... tokens) {
		this.trace (ExceptionResolution.Ignored, exception, format, tokens);
	}
	
	public final UncaughtExceptionHandler catcher;
}
