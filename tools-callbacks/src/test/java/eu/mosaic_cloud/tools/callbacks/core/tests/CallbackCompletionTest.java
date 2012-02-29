
package eu.mosaic_cloud.tools.callbacks.core.tests;


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import org.junit.Assert;
import org.junit.Test;


public final class CallbackCompletionTest
{
	@Test
	public final void testFailure ()
	{
		final Throwable exception = new Throwable ();
		final CallbackCompletion<Void> completion = CallbackCompletion.createFailure (exception);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertSame (exception, completion.getException ());
		try {
			completion.getOutcome ();
			Assert.fail ();
		} catch (final IllegalStateException exception1) {
			// NOTE: expected behavior
		}
		Assert.assertTrue (this.awaitFailure (completion));
	}
	
	@Test
	public final void testOutcome ()
	{
		final CallbackCompletion<Void> completion = CallbackCompletion.createOutcome ();
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertNull (completion.getException ());
		Assert.assertNull (completion.getOutcome ());
		Assert.assertTrue (this.awaitSuccess (completion));
	}
	
	protected final boolean awaitBooleanOutcome (final CallbackCompletion<Boolean> completion)
	{
		CallbackCompletionTest.await (completion, this.defaultPoolTimeout);
		return this.getBooleanOutcome (completion);
	}
	
	protected final <_Outcome_ extends Object> _Outcome_ awaitOutcome (final CallbackCompletion<_Outcome_> completion)
	{
		CallbackCompletionTest.await (completion, this.defaultPoolTimeout);
		return this.getOutcome (completion);
	}
	
	protected final boolean awaitSuccess (final CallbackCompletion<?> completion)
	{
		CallbackCompletionTest.await (completion, this.defaultPoolTimeout);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return true;
	}

	protected final boolean awaitFailure (final CallbackCompletion<?> completion)
	{
		CallbackCompletionTest.await (completion, this.defaultPoolTimeout);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertNotNull (completion.getException ());
		return true;
	}
	
	protected final boolean getBooleanOutcome (final CallbackCompletion<Boolean> completion)
	{
		final Boolean value = this.getOutcome (completion);
		Assert.assertNotNull (value);
		return value.booleanValue ();
	}
	
	protected final <_Outcome_ extends Object> _Outcome_ getOutcome (final CallbackCompletion<_Outcome_> completion)
	{
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return completion.getOutcome ();
	}
	
	public final long defaultPoolTimeout = 1000;
	
	protected static final void await (final CallbackCompletion<?> completion, final long timeout)
	{
		Assert.assertTrue (completion.await (timeout));
	}
}
