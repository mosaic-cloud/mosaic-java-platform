
package eu.mosaic_cloud.exceptions.tools;


import eu.mosaic_cloud.exceptions.core.CaughtException;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public abstract class BaseExceptionTracer
		extends Object
		implements
			ExceptionTracer
{
	protected BaseExceptionTracer ()
	{
		super ();
	}
	
	public final void traceCaughtException (final CaughtException exception)
	{
		exception.trace (this);
	}
	
	public final void traceDeferredException (final Throwable exception)
	{
		this.trace (ExceptionResolution.Deferred, exception);
	}
	
	public final void traceDeferredException (final Throwable exception, final String message)
	{
		this.trace (ExceptionResolution.Deferred, exception, message);
	}
	
	public final void traceDeferredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (ExceptionResolution.Deferred, exception, format, tokens);
	}
	
	public final void traceHandledException (final Throwable exception)
	{
		this.trace (ExceptionResolution.Handled, exception);
	}
	
	public final void traceHandledException (final Throwable exception, final String message)
	{
		this.trace (ExceptionResolution.Handled, exception, message);
	}
	
	public final void traceHandledException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (ExceptionResolution.Handled, exception, format, tokens);
	}
	
	public final void traceIgnoredException (final Throwable exception)
	{
		this.trace (ExceptionResolution.Ignored, exception);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String message)
	{
		this.trace (ExceptionResolution.Ignored, exception, message);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (ExceptionResolution.Ignored, exception, format, tokens);
	}
}
