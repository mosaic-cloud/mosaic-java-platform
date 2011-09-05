
package eu.mosaic_cloud.exceptions.tools;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.CaughtException;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public final class QueueingExceptionTracer
		extends InterceptingExceptionTracer
{
	private QueueingExceptionTracer (final BlockingQueue<CaughtException> queue, final ExceptionTracer delegate)
	{
		super (delegate);
		Preconditions.checkNotNull (queue);
		this.queue = queue;
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception)
	{
		this.queue.add (new CaughtException (resolution, exception));
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.queue.add (new CaughtException (resolution, exception, message));
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.queue.add (new CaughtException (resolution, exception, format, tokens));
	}
	
	public final BlockingQueue<CaughtException> queue;
	
	public static final QueueingExceptionTracer create (final BlockingQueue<CaughtException> queue, final ExceptionTracer delegate)
	{
		return (new QueueingExceptionTracer (queue, delegate));
	}
	
	public static final QueueingExceptionTracer create (final ExceptionTracer delegate)
	{
		return (new QueueingExceptionTracer (new LinkedBlockingQueue<CaughtException> (), delegate));
	}
}
