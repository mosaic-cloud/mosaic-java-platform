
package eu.mosaic_cloud.exceptions.tools;


import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public class NullExceptionTracer
		extends Object
		implements
			ExceptionTracer
{
	private NullExceptionTracer ()
	{
		super ();
	}
	
	@Override
	public void trace (final ExceptionResolution resolution, final Throwable exception)
	{}
	
	@Override
	public void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{}
	
	@Override
	public void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{}
	
	public static final NullExceptionTracer defaultInstance = new NullExceptionTracer ();
}
