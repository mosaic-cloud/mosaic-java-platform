/*
 * #%L
 * mosaic-tools-exceptions
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.tools.exceptions.tools;


import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;

import com.google.common.base.Preconditions;


public final class QueueingExceptionTracer
		extends DelegatingExceptionTracer
{
	private QueueingExceptionTracer (final BlockingQueue<CaughtException> queue, final long waitTimeout, final ExceptionTracer delegate)
	{
		super (delegate);
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		this.queue = queue;
		this.waitTimeout = waitTimeout;
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception)
	{
		this.enqueue (new CaughtException (resolution, exception));
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.enqueue (new CaughtException (resolution, exception, message));
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.enqueue (new CaughtException (resolution, exception, format, tokens));
	}
	
	private final void enqueue (final CaughtException exception)
	{
		if (exception.getResolution () == ExceptionResolution.Handled)
			return;
		try {
			final boolean enqueued;
			if (this.waitTimeout > 0)
				enqueued = this.queue.offer (exception, this.waitTimeout, TimeUnit.MILLISECONDS);
			else if (this.waitTimeout == 0)
				enqueued = this.queue.offer (exception);
			else if (this.waitTimeout == -1) {
				this.queue.put (exception);
				enqueued = true;
			} else
				throw (new AssertionError ());
			if (!enqueued)
				throw (new BufferOverflowException ());
		} catch (final InterruptedException exception1) {
			throw (new BufferOverflowException ());
		}
	}
	
	public static final QueueingExceptionTracer create (final BlockingQueue<CaughtException> queue, final long waitTimeout, final ExceptionTracer delegate)
	{
		return (new QueueingExceptionTracer (queue, waitTimeout, delegate));
	}
	
	public static final QueueingExceptionTracer create (final ExceptionTracer delegate)
	{
		return (new QueueingExceptionTracer (new LinkedBlockingQueue<CaughtException> (), 0, delegate));
	}
	
	public final BlockingQueue<CaughtException> queue;
	private final long waitTimeout;
}
