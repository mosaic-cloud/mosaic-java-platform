/*
 * #%L
 * mosaic-tools-exceptions
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.tools.exceptions.core.CaughtException;

import com.google.common.base.Preconditions;


public final class QueuedExceptions
			extends Exception
{
	private QueuedExceptions (final BlockingQueue<CaughtException> queue) {
		super ();
		this.queue = queue;
		Preconditions.checkNotNull (queue);
	}
	
	public final BlockingQueue<CaughtException> queue;
	
	public static final QueuedExceptions create (final BlockingQueue<CaughtException> queue) {
		return (new QueuedExceptions (queue));
	}
	
	public static final QueuedExceptions create (final QueueingExceptionTracer exceptions) {
		final LinkedBlockingQueue<CaughtException> queue = new LinkedBlockingQueue<CaughtException> ();
		exceptions.queue.drainTo (queue);
		return (QueuedExceptions.create (queue));
	}
	
	private static final long serialVersionUID = 1L;
}
