/*
 * #%L
 * mosaic-tools-exceptions
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
