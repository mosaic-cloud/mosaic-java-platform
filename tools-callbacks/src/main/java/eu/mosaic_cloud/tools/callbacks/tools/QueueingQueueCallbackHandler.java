/*
 * #%L
 * mosaic-tools-callbacks
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

package eu.mosaic_cloud.tools.callbacks.tools;


import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import com.google.common.base.Preconditions;


public final class QueueingQueueCallbackHandler<_Element_ extends Object>
			extends Object
			implements
				QueueCallbacks<_Element_>,
				CallbackHandler
{
	private QueueingQueueCallbackHandler (final BlockingQueue<_Element_> queue, final long waitTimeout, final ExceptionTracer exceptions) {
		super ();
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((waitTimeout >= 0) || (waitTimeout == -1));
		Preconditions.checkNotNull (exceptions);
		this.queue = queue;
		this.waitTimeout = waitTimeout;
		this.exceptions = exceptions;
	}
	
	@Override
	public final CallbackCompletion<Void> enqueue (final _Element_ element) {
		if (!Threading.offer (this.queue, element, this.waitTimeout))
			throw (new BufferOverflowException ());
		return (null);
	}
	
	@Override
	public final void failedCallbacks (final Callbacks proxy, final Throwable exception) {
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate) {}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks proxy) {}
	
	public final BlockingQueue<_Element_> queue;
	private final ExceptionTracer exceptions;
	private final long waitTimeout;
	
	public static final <_Element_ extends Object> QueueingQueueCallbackHandler<_Element_> create (final BlockingQueue<_Element_> queue, final long waitTimeout, final ExceptionTracer exceptions) {
		return (new QueueingQueueCallbackHandler<_Element_> (queue, waitTimeout, exceptions));
	}
	
	public static final <_Element_ extends Object> QueueingQueueCallbackHandler<_Element_> create (final ExceptionTracer exceptions) {
		return (new QueueingQueueCallbackHandler<_Element_> (new LinkedBlockingQueue<_Element_> (), 0, exceptions));
	}
}
