/*
 * #%L
 * mosaic-tools-callbacks
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

package eu.mosaic_cloud.tools.callbacks.implementations.basic.tests;


import java.util.LinkedList;

import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.v2.BasicCallbackReactor;
import eu.mosaic_cloud.tools.callbacks.tools.QueueCallbacks;
import eu.mosaic_cloud.tools.callbacks.tools.QueueingQueueCallbackHandler;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

import org.junit.Assert;
import org.junit.Test;


public final class BasicCallbackReactorTest
{
	@Test
	public final void test ()
	{
		BasicThreadingSecurityManager.initialize ();
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions.catcher);
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (threading, exceptions);
		Assert.assertTrue (reactor.initialize (BasicCallbackReactorTest.defaultPollTimeout));
		final CallbackIsolate isolate = reactor.createIsolate ();
		Assert.assertNotNull (isolate);
		final LinkedList<QueueCallbacks<Integer>> triggers = new LinkedList<QueueCallbacks<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.defaultQueueCount; index++) {
			final QueueCallbacks<Integer> proxy = reactor.createProxy (QueueCallbacks.class);
			triggers.add (proxy);
		}
		final LinkedList<CallbackReference> callbacks = new LinkedList<CallbackReference> ();
		{
			int counter = 0;
			for (int index = 0; index < BasicCallbackReactorTest.defaultCallCount; index++) {
				for (final QueueCallbacks<Integer> trigger : triggers) {
					final CallbackReference callback = trigger.enqueue (Integer.valueOf (counter));
					callbacks.add (callback);
					counter++;
				}
			}
		}
		final LinkedList<QueueingQueueCallbackHandler<Integer>> handlers = new LinkedList<QueueingQueueCallbackHandler<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.defaultQueueCount; index++) {
			final QueueingQueueCallbackHandler<Integer> handler = QueueingQueueCallbackHandler.create (exceptions);
			final CallbackReference callback = reactor.assignHandler (triggers.get (index), handler, isolate);
			handlers.add (handler);
			callbacks.add (callback);
		}
		for (final CallbackReference reference : callbacks)
			Assert.assertTrue (reference.await (BasicCallbackReactorTest.defaultPollTimeout));
		{
			int counter = 0;
			for (int index = 0; index < BasicCallbackReactorTest.defaultCallCount; index++)
				for (final QueueingQueueCallbackHandler<Integer> handler : handlers) {
					Assert.assertEquals (Integer.valueOf (counter), handler.queue.poll ());
					counter++;
				}
			for (final QueueingQueueCallbackHandler<Integer> handler : handlers)
				Assert.assertNull (handler.queue.poll ());
		}
		isolate.destroy ();
		Assert.assertTrue (isolate.await (BasicCallbackReactorTest.defaultPollTimeout));
		reactor.destoy ();
		Assert.assertTrue (reactor.await (BasicCallbackReactorTest.defaultPollTimeout));
		Assert.assertTrue (threading.await (BasicCallbackReactorTest.defaultPollTimeout));
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	public static final int defaultCallCount = 16;
	public static final long defaultPollTimeout = 1000;
	public static final int defaultQueueCount = 16;
}
