
package eu.mosaic_cloud.exceptions.tools;


import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public abstract class InterceptingExceptionTracer
		extends BaseExceptionTracer
{
	protected InterceptingExceptionTracer (final ExceptionTracer delegate)
	{
		super ();
		this.delegate = delegate;
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		try {
			this.trace_ (resolution, exception);
		} catch (final Throwable exception1) {}
		try {
			final ExceptionTracer delegate = (this.delegate != null) ? this.delegate : ExceptionTracer.defaultInstance.get ();
			if (delegate != null)
				delegate.trace (resolution, exception);
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		try {
			this.trace_ (resolution, exception, message);
		} catch (final Throwable exception1) {}
		try {
			final ExceptionTracer delegate = (this.delegate != null) ? this.delegate : ExceptionTracer.defaultInstance.get ();
			if (delegate != null)
				delegate.trace (resolution, exception, message);
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		try {
			this.trace_ (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {}
		try {
			final ExceptionTracer delegate = (this.delegate != null) ? this.delegate : ExceptionTracer.defaultInstance.get ();
			if (delegate != null)
				delegate.trace (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {}
	}
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception);
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message);
	
	protected abstract void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens);
	
	private final ExceptionTracer delegate;
}
