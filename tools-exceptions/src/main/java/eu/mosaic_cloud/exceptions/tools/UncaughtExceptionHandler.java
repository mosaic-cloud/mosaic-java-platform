
package eu.mosaic_cloud.exceptions.tools;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public final class UncaughtExceptionHandler
		implements
			Thread.UncaughtExceptionHandler
{
	private UncaughtExceptionHandler (final ExceptionTracer delegate)
	{
		super ();
		Preconditions.checkNotNull (delegate);
		this.delegate = delegate;
	}
	
	@Override
	public final void uncaughtException (final Thread thread, final Throwable exception)
	{
		this.delegate.trace (ExceptionResolution.Ignored, exception);
	}
	
	private final ExceptionTracer delegate;
	
	public static final UncaughtExceptionHandler create (final ExceptionTracer delegate)
	{
		return (new UncaughtExceptionHandler (delegate));
	}
}
