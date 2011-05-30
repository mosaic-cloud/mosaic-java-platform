
package eu.mosaic_cloud.callbacks.tests;


import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.callbacks.core.CallbackFuture;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.callbacks.tools.QueueCallbacks;
import eu.mosaic_cloud.callbacks.tools.QueueingQueueCallbacks;

import org.junit.Assert;
import org.junit.Test;


public final class BasicCallbackReactorTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final int queueCount = 16;
		final int callCount = 16;
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		reactor.initialize ();
		final LinkedList<QueueCallbacks<Integer>> triggers = new LinkedList<QueueCallbacks<Integer>> ();
		for (int index = 0; index < queueCount; index++) {
			final QueueCallbacks<Integer> trigger = reactor.register (QueueCallbacks.class, null);
			triggers.add (trigger);
		}
		final ConcurrentLinkedQueue<CallbackFuture> futures = new ConcurrentLinkedQueue<CallbackFuture> ();
		{
			int counter = 0;
			for (int index = 0; index < callCount; index++) {
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
		final LinkedList<LinkedBlockingQueue<Integer>> queues = new LinkedList<LinkedBlockingQueue<Integer>> ();
		for (int index = 0; index < queueCount; index++) {
			final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer> ();
			final QueueingQueueCallbacks<Integer> callbacks = new QueueingQueueCallbacks<Integer> (queue);
			queues.add (queue);
			reactor.assign (triggers.get (index), callbacks);
		}
		for (final CallbackFuture future : futures)
			Assert.assertNull (future.get ());
		Thread.sleep (1000);
		{
			int counter = 0;
			for (int index = 0; index < callCount; index++)
				for (final LinkedBlockingQueue<Integer> queue : queues) {
					Assert.assertEquals (Integer.valueOf (counter), queue.poll ());
					counter++;
				}
			for (final LinkedBlockingQueue<Integer> queue : queues)
				Assert.assertNull (queue.poll ());
		}
		reactor.terminate ();
	}
}
