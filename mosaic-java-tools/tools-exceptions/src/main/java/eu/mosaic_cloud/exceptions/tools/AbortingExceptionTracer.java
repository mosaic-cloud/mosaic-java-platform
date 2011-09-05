
package eu.mosaic_cloud.exceptions.tools;


import eu.mosaic_cloud.exceptions.core.ExceptionResolution;


public final class AbortingExceptionTracer
		extends BaseExceptionTracer
{
	private AbortingExceptionTracer ()
	{
		super ();
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		final boolean abort;
		switch (resolution) {
			case Handled :
				abort = false;
				break;
			case Deferred :
				abort = false;
				break;
			case Ignored :
				abort = true;
				break;
			default:
				abort = true;
				break;
		}
		if (abort) {
			try {
				exception.printStackTrace (System.err);
			} catch (final Throwable exception1) {}
			System.exit (1);
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.trace (resolution, exception);
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (resolution, exception);
	}
	
	public static final AbortingExceptionTracer defaultInstance = new AbortingExceptionTracer ();
}
