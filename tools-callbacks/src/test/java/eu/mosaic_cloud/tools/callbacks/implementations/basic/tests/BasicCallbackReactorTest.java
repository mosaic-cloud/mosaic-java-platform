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

package eu.mosaic_cloud.tools.callbacks.implementations.basic.tests;


import java.util.LinkedList;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.callbacks.tools.QueueCallbacks;
import eu.mosaic_cloud.tools.callbacks.tools.QueueingQueueCallbackHandler;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public final class BasicCallbackReactorTest
{
	@Test
	public final void test ()
	{
		final Transcript transcript = Transcript.create (this);
		BasicThreadingSecurityManager.initialize ();
		final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, exceptionsQueue);
		final BasicThreadingContext threading = BasicThreadingContext.create (this, exceptions, exceptions.catcher);
		Assert.assertTrue (threading.initialize (BasicCallbackReactorTest.defaultPollTimeout));
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (threading, exceptions);
		Assert.assertTrue (reactor.initialize (BasicCallbackReactorTest.defaultPollTimeout));
		final CallbackIsolate isolate = reactor.createIsolate ();
		Assert.assertNotNull (isolate);
		final LinkedList<QueueCallbacks<Integer>> triggers = new LinkedList<QueueCallbacks<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.defaultQueueCount; index++) {
			final QueueCallbacks<Integer> proxy = reactor.createProxy (QueueCallbacks.class);
			triggers.add (proxy);
		}
		final LinkedList<CallbackCompletion<Void>> completions = new LinkedList<CallbackCompletion<Void>> ();
		{
			int counter = 0;
			for (int index = 0; index < BasicCallbackReactorTest.defaultCallCount; index++) {
				for (final QueueCallbacks<Integer> trigger : triggers) {
					final CallbackCompletion<Void> completion = trigger.enqueue (Integer.valueOf (counter));
					completions.add (completion);
					counter++;
				}
			}
		}
		final LinkedList<QueueingQueueCallbackHandler<Integer>> handlers = new LinkedList<QueueingQueueCallbackHandler<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.defaultQueueCount; index++) {
			final QueueingQueueCallbackHandler<Integer> handler = QueueingQueueCallbackHandler.create (exceptions);
			final CallbackCompletion<Void> completion = reactor.assignHandler (triggers.get (index), handler, isolate);
			handlers.add (handler);
			completions.add (completion);
		}
		for (final CallbackCompletion<Void> completion : completions)
			Assert.assertTrue (completion.await (BasicCallbackReactorTest.defaultPollTimeout));
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
		Assert.assertTrue (isolate.destroy ().await (BasicCallbackReactorTest.defaultPollTimeout));
		Assert.assertTrue (reactor.destroy (BasicCallbackReactorTest.defaultPollTimeout));
		Assert.assertTrue (threading.destroy (BasicCallbackReactorTest.defaultPollTimeout));
		Assert.assertNull (exceptionsQueue.queue.poll ());
	}
	
	public static final int defaultCallCount = 16;
	public static final long defaultPollTimeout = 1000;
	public static final int defaultQueueCount = 16;
}
