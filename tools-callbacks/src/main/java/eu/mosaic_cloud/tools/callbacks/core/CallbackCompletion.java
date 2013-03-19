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

package eu.mosaic_cloud.tools.callbacks.core;


import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionAndChainedBackend;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionFutureBackend;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.Joinable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;


public final class CallbackCompletion<_Outcome_ extends Object>
		extends Object
		implements
			Joinable
{
	// FIXME: On creation the completion should "capture" the "current" exceptions tracer and store it.
	//-- Then whenever an exception is encountered, whatever the thread may be, it should be directed to that tracer.
	//-- (Or maybe this tracer should be determined at creation through the `backend`.)
	private CallbackCompletion (final _Outcome_ outcome)
	{
		super ();
		this.backend = null;
		this.exception = null;
		this.outcome = outcome;
	}
	
	private CallbackCompletion (final CallbackCompletionBackend backend)
	{
		super ();
		Preconditions.checkNotNull (backend);
		this.backend = backend;
		this.exception = null;
		this.outcome = (_Outcome_) CallbackCompletion.unknownOutcome;
	}
	
	private CallbackCompletion (final Throwable exception)
	{
		super ();
		Preconditions.checkNotNull (exception);
		this.backend = null;
		this.exception = exception;
		this.outcome = (_Outcome_) CallbackCompletion.exceptionOutcome;
	}
	
	@Override
	public final boolean await ()
	{
		return (this.await (-1));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		if (this.outcome != CallbackCompletion.unknownOutcome)
			return (true);
		assert (this.backend != null);
		try {
			return (this.backend.awaitCompletion (this, timeout));
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			return (false);
		}
	}
	
	public final Throwable getException ()
	{
		if (!this.isCompleted ())
			throw (new IllegalStateException ());
		if (this.outcome != CallbackCompletion.exceptionOutcome)
			return (null);
		assert (this.exception != null);
		return (this.exception);
	}
	
	public final _Outcome_ getOutcome ()
	{
		if (!this.isCompleted ())
			throw (new IllegalStateException ());
		if (this.outcome == CallbackCompletion.exceptionOutcome)
			throw (new IllegalStateException ());
		assert (this.exception == null);
		return (this.outcome);
	}
	
	public final CallbackReactor getReactor ()
	{
		if (this.backend == null)
			return (null);
		try {
			return (this.backend.getReactor ());
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			return (null);
		}
	}
	
	public final boolean isCompleted ()
	{
		if (this.outcome == CallbackCompletion.unknownOutcome)
			try {
				if (!this.backend.awaitCompletion (this, 0))
					return (false);
			} catch (final Throwable exception) {
				// FIXME: See the `FIXME` notice at the top of the class.
				FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
				return (false);
			}
		else if (this.outcome == CallbackCompletion.exceptionOutcome)
			return (true);
		else
			return (true);
		assert (this.backend != null);
		try {
			final Throwable exception = this.backend.getCompletionException (this);
			if (exception == null)
				this.outcome = (_Outcome_) this.backend.getCompletionOutcome (this);
			else {
				this.exception = exception;
				this.outcome = (_Outcome_) CallbackCompletion.exceptionOutcome;
			}
			return (true);
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			return (false);
		}
	}
	
	public final void observe (final CallbackCompletionObserver observer)
	{
		if (this.backend != null) {
			Preconditions.checkNotNull (observer);
			// FIXME: We should enforce that the observer is also a callback proxy.
			//-- Currently this is a hack to "ease" the development.
			//# Preconditions.checkArgument (observer instanceof CallbackProxy);
			try {
				this.backend.observeCompletion (this, observer);
			} catch (final Throwable exception) {
				// FIXME: See the `FIXME` notice at the top of the class.
				FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
			}
		} else
			CallbackCompletion.triggerObserver (this, observer);
	}
	
	public static final CallbackCompletion<Void> createAndChained (final CallbackCompletion<?> ... dependents)
	{
		return (CallbackCompletionAndChainedBackend.createCompletion (dependents));
	}
	
	public static final CallbackCompletion<Void> createChained (final CallbackCompletion<?> ... dependents)
	{
		return (CallbackCompletionAndChainedBackend.createCompletion (dependents));
	}
	
	public static final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> createDeferred (final CallbackCompletionBackend backend)
	{
		return (new CallbackCompletion<_Outcome_> (backend));
	}
	
	public static final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> createDeferred (final ListenableFuture<_Outcome_> future)
	{
		return (CallbackCompletionFutureBackend.createCompletion (future));
	}
	
	public static final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> createFailure (final Throwable exception)
	{
		return (new CallbackCompletion<_Outcome_> (exception));
	}
	
	public static final CallbackCompletion<Void> createOutcome ()
	{
		return (CallbackCompletion.createOutcome ((Void) null));
	}
	
	public static final <_Outcome_ extends Object> CallbackCompletion<_Outcome_> createOutcome (final _Outcome_ outcome)
	{
		return (new CallbackCompletion<_Outcome_> (outcome));
	}
	
	public static final CallbackCompletion<Boolean> createOutcome (final boolean outcome)
	{
		return (CallbackCompletion.createOutcome (Boolean.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Byte> createOutcome (final byte outcome)
	{
		return (CallbackCompletion.createOutcome (Byte.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Character> createOutcome (final char outcome)
	{
		return (CallbackCompletion.createOutcome (Character.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Double> createOutcome (final double outcome)
	{
		return (CallbackCompletion.createOutcome (Double.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Float> createOutcome (final float outcome)
	{
		return (CallbackCompletion.createOutcome (Float.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Integer> createOutcome (final int outcome)
	{
		return (CallbackCompletion.createOutcome (Integer.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<Long> createOutcome (final long outcome)
	{
		return (CallbackCompletion.createOutcome (Long.valueOf (outcome)));
	}
	
	public static final CallbackCompletion<String> createOutcome (final String outcome)
	{
		return (CallbackCompletion.createOutcome (outcome));
	}
	
	public static final void triggerObserver (final CallbackCompletion<?> completion, final CallbackCompletionObserver observer)
	{
		Preconditions.checkNotNull (completion);
		Preconditions.checkState (completion.isCompleted ());
		Preconditions.checkNotNull (observer);
		// FIXME: We should enforce that the observer is also a callback proxy.
		//-- Currently this is a hack to "ease" the development.
		//# Preconditions.checkArgument (observer instanceof CallbackProxy);
		try {
			observer.completed (completion);
		} catch (final Throwable exception) {
			// FIXME: See the `FIXME` notice at the top of the class.
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception);
		}
	}
	
	final CallbackCompletionBackend backend;
	private volatile Throwable exception;
	private volatile _Outcome_ outcome;
	private static final Object exceptionOutcome = new Object ();
	private static final Object unknownOutcome = new Object ();
}
