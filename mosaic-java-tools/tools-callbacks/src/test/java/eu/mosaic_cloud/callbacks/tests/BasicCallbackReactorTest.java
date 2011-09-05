
package eu.mosaic_cloud.callbacks.tests;


import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.callbacks.core.CallbackFuture;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.callbacks.tools.QueueCallbacks;
import eu.mosaic_cloud.callbacks.tools.QueueingQueueCallbacks;
import eu.mosaic_cloud.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;

import org.junit.Assert;
import org.junit.Test;


public final class BasicCallbackReactorTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (exceptions);
		reactor.initialize ();
		final LinkedList<QueueCallbacks<Integer>> triggers = new LinkedList<QueueCallbacks<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.queueCount; index++) {
			final QueueCallbacks<Integer> trigger = reactor.register (QueueCallbacks.class, null);
			triggers.add (trigger);
		}
		final ConcurrentLinkedQueue<CallbackFuture> futures = new ConcurrentLinkedQueue<CallbackFuture> ();
		{
			int counter = 0;
			for (int index = 0; index < BasicCallbackReactorTest.callCount; index++) {
				for (final QueueCallbacks<Integer> trigger : triggers) {
					final CallbackReference reference = trigger.enqueue (Integer.valueOf (counter));
					final CallbackFuture future = reactor.resolve (reference);
					futures.add (future);
					counter++;
				}
			}
		}
		for (final QueueCallbacks<Integer> trigger : triggers) {
			final CallbackReference reference = trigger.enqueue (Integer.valueOf (-1));
			final CallbackFuture future = reactor.resolve (reference);
			Assert.assertTrue (future.cancel (false));
		}
		final LinkedList<QueueingQueueCallbacks<Integer>> callbacks = new LinkedList<QueueingQueueCallbacks<Integer>> ();
		for (int index = 0; index < BasicCallbackReactorTest.queueCount; index++) {
			final QueueingQueueCallbacks<Integer> callback = QueueingQueueCallbacks.create ();
			callbacks.add (callback);
			reactor.assign (triggers.get (index), callback);
		}
		for (final CallbackFuture future : futures)
			Assert.assertNull (future.get (BasicCallbackReactorTest.pollTimeout, TimeUnit.MILLISECONDS));
		Thread.sleep (BasicCallbackReactorTest.sleepTimeout);
		{
			int counter = 0;
			for (int index = 0; index < BasicCallbackReactorTest.callCount; index++)
				for (final QueueingQueueCallbacks<Integer> callback : callbacks) {
					Assert.assertEquals (Integer.valueOf (counter), callback.queue.poll ());
					counter++;
				}
			for (final QueueingQueueCallbacks<Integer> callback : callbacks)
				Assert.assertNull (callback.queue.poll ());
		}
		reactor.terminate ();
		Thread.sleep (BasicCallbackReactorTest.sleepTimeout);
		Assert.assertNull (exceptions.queue.poll ());
	}
	
	private static final int callCount = 16;
	private static final long pollTimeout = 1000;
	private static final int queueCount = 16;
	private static final long sleepTimeout = 100;
}
