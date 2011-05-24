
package eu.mosaic_cloud.callbacks.tests;


import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import eu.mosaic_cloud.callbacks.core.CallbackFuture;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;

import org.junit.Assert;
import org.junit.Test;


public final class BasicCallbackReactorTest
{
	@Test
	public final void test ()
			throws Exception
	{
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		final LinkedList<Queue> queues = new LinkedList<Queue> ();
		final LinkedList<QueueCallbacks> triggers = new LinkedList<QueueCallbacks> ();
		for (int index = 0; index < 4; index++) {
			final Queue queue = new Queue ();
			final QueueCallbacks trigger = reactor.register (QueueCallbacks.class, queue);
			queues.add (queue);
			triggers.add (trigger);
		}
		reactor.start ();
		final ConcurrentLinkedQueue<CallbackFuture> futures = new ConcurrentLinkedQueue<CallbackFuture> ();
		final int tries = 16;
		for (int index = 0; index < tries; index++) {
			for (final QueueCallbacks trigger : triggers) {
				final CallbackReference reference = trigger.enqueue (Integer.valueOf (index));
				final CallbackFuture future = reactor.resolve (reference);
				futures.add (future);
			}
		}
		for (final QueueCallbacks trigger : triggers) {
			final CallbackReference reference = trigger.enqueue (Integer.valueOf (-1));
			final CallbackFuture future = reactor.resolve (reference);
			Assert.assertTrue (future.cancel (false));
		}
		for (final CallbackFuture future : futures)
			Assert.assertNull (future.get ());
		reactor.stop ();
		Thread.sleep (1000);
		for (final Queue queue : queues) {
			for (int index = 0; index < tries; index++)
				Assert.assertEquals (Integer.valueOf (index), queue.poll ());
			Assert.assertNull (queue.poll ());
		}
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Assert.assertTrue (arguments.length == 0);
		final BasicCallbackReactorTest tests = new BasicCallbackReactorTest ();
		tests.test ();
	}
	
	static final Random random = new Random ();
	
	public static final class Queue
			extends ConcurrentLinkedQueue<Integer>
			implements
				QueueCallbacks
	{
		@Override
		public final CallbackReference enqueue (final Integer value)
				throws Exception
		{
			Thread.sleep (BasicCallbackReactorTest.random.nextInt (100));
			this.add (value);
			return (null);
		}
		
		private static final long serialVersionUID = 1L;
	}
	
	public static interface QueueCallbacks
			extends
				Callbacks
	{
		public abstract CallbackReference enqueue (final Integer value)
				throws Exception;
	}
}
