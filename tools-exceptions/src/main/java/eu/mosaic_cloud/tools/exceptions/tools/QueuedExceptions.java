
package eu.mosaic_cloud.tools.exceptions.tools;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.tools.exceptions.core.CaughtException;

import com.google.common.base.Preconditions;


public final class QueuedExceptions
		extends Exception
{
	private QueuedExceptions (final BlockingQueue<CaughtException> queue)
	{
		super ();
		this.queue = queue;
		Preconditions.checkNotNull (queue);
	}
	
	public final BlockingQueue<CaughtException> queue;
	
	public static final QueuedExceptions create (final BlockingQueue<CaughtException> queue)
	{
		return (new QueuedExceptions (queue));
	}
	
	public static final QueuedExceptions create (final QueueingExceptionTracer exceptions)
	{
		final LinkedBlockingQueue<CaughtException> queue = new LinkedBlockingQueue<CaughtException> ();
		exceptions.queue.drainTo (queue);
		return (QueuedExceptions.create (queue));
	}
	
	private static final long serialVersionUID = 1L;
}
