
package eu.mosaic_cloud.exceptions.core;


import java.util.concurrent.atomic.AtomicReference;


public interface ExceptionTracer
{
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String message);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens);
	
	public static final AtomicReference<ExceptionTracer> defaultInstance = new AtomicReference<ExceptionTracer> (null);
}
