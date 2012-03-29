
package eu.mosaic_cloud.tools.callbacks.tools.tests;


import java.util.concurrent.atomic.AtomicBoolean;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;


public final class CallbackCompletionFutureBackendTest
{
	@Test
	public final void testFailed ()
	{
		final Throwable marker = new Throwable ();
		final AtomicBoolean observed1 = new AtomicBoolean (false);
		final AtomicBoolean observed2 = new AtomicBoolean (false);
		final SettableFuture<Object> future = SettableFuture.create ();
		final CallbackCompletion<Object> completion = CallbackCompletion.createDeferred (future);
		Assert.assertFalse (completion.await (0));
		Assert.assertFalse (completion.await (10));
		completion.observe (new Observer (completion, observed1));
		future.setException (marker);
		Assert.assertTrue (completion.await (0));
		completion.observe (new Observer (completion, observed2));
		Assert.assertSame (marker, completion.getException ());
		try {
			completion.getOutcome ();
			throw (new AssertionError ());
		} catch (final Throwable exception) {
			Assert.assertTrue (exception instanceof IllegalStateException);
		}
		Assert.assertTrue (observed1.get ());
		Assert.assertTrue (observed2.get ());
	}
	
	@Test
	public final void testSucceeded ()
	{
		final Object marker = new Object ();
		final AtomicBoolean observed1 = new AtomicBoolean (false);
		final AtomicBoolean observed2 = new AtomicBoolean (false);
		final SettableFuture<Object> future = SettableFuture.create ();
		final CallbackCompletion<Object> completion = CallbackCompletion.createDeferred (future);
		Assert.assertFalse (completion.await (0));
		Assert.assertFalse (completion.await (10));
		completion.observe (new Observer (completion, observed1));
		future.set (marker);
		Assert.assertTrue (completion.await (0));
		completion.observe (new Observer (completion, observed2));
		Assert.assertNull (completion.getException ());
		Assert.assertSame (marker, completion.getOutcome ());
		Assert.assertTrue (observed1.get ());
		Assert.assertTrue (observed2.get ());
	}
	
	final class Observer
			extends Object
			implements
				CallbackCompletionObserver
	{
		Observer (final CallbackCompletion<?> completion, final AtomicBoolean triggered)
		{
			super ();
			this.completion = completion;
			this.obvserved = triggered;
		}
		
		@Override
		public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion)
		{
			Preconditions.checkArgument (completion == this.completion);
			Preconditions.checkState (this.obvserved.compareAndSet (false, true));
			return (CallbackCompletion.createOutcome ());
		}
		
		final CallbackCompletion<?> completion;
		final AtomicBoolean obvserved;
	}
}
