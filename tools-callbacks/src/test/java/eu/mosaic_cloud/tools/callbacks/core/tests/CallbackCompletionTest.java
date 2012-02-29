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
