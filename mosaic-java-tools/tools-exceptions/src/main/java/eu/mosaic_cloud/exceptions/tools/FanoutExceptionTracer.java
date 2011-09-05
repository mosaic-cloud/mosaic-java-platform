
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
			} catch (final Throwable exception1) {}
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		for (final ExceptionTracer delegate : this.delegates)
			try {
				delegate.trace (resolution, exception, message);
			} catch (final Throwable exception1) {}
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		for (final ExceptionTracer delegate : this.delegates)
			try {
				delegate.trace (resolution, exception, format, tokens);
			} catch (final Throwable exception1) {}
	}
	
	private final ExceptionTracer[] delegates;
	
	public static final FanoutExceptionTracer create (final ExceptionTracer ... delegates)
	{
		return (new FanoutExceptionTracer (delegates));
	}
}
