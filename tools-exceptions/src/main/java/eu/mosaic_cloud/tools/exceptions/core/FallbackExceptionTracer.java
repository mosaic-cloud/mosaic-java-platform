
package eu.mosaic_cloud.tools.exceptions.core;


import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.InterceptingExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class FallbackExceptionTracer
		extends InterceptingExceptionTracer
{
	private FallbackExceptionTracer ()
	{
		super ();
		this.globalDelegate = Atomics.newReference (null);
		this.threadDelegate = new InheritableThreadLocal<ExceptionTracer> ();
	}
	
	public final void setGlobalTracer (final ExceptionTracer tracer)
	{
		Preconditions.checkNotNull (tracer);
		Preconditions.checkArgument (tracer != this);
		this.globalDelegate.set (tracer);
	}
	
	public final void setThreadTracer (final ExceptionTracer tracer)
	{
		Preconditions.checkNotNull (tracer);
		Preconditions.checkArgument (tracer != this);
		this.threadDelegate.set (tracer);
	}
	
	@Override
	protected final ExceptionTracer getDelegate ()
	{
		{
			final ExceptionTracer delegate = this.globalDelegate.get ();
			if (delegate != null)
				return (delegate);
		}
		{
			final ExceptionTracer delegate = this.threadDelegate.get ();
			if (delegate != null)
				return (delegate);
		}
		return (AbortingExceptionTracer.defaultInstance);
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception)
	{
		// NOTE: intentional
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		// NOTE: intentional
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		// NOTE: intentional
	}
	
	private final AtomicReference<ExceptionTracer> globalDelegate;
	private final InheritableThreadLocal<ExceptionTracer> threadDelegate;
	public static final FallbackExceptionTracer defaultInstance = new FallbackExceptionTracer ();
}
