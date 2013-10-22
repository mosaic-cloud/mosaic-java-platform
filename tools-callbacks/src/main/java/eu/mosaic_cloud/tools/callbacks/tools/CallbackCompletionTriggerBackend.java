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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionBackend;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class CallbackCompletionTriggerBackend<_Outcome_ extends Object>
			implements
				CallbackCompletionBackend
{
	private CallbackCompletionTriggerBackend () {
		super ();
		this.pending = new CountDownLatch (1);
		this.outcome = Atomics.newReference ();
		this.exception = Atomics.newReference ();
		this.observers = new LinkedBlockingQueue<CallbackCompletionObserver> ();
		this.completion = CallbackCompletion.createDeferred (this);
		this.trigger = new CallbackCompletionTrigger (this);
	}
	
	@Override
	public final boolean awaitCompletion (final CallbackCompletion<?> completion, final long timeout) {
		Preconditions.checkArgument (completion == this.completion);
		return (Threading.await (this.pending, timeout));
	}
	
	@Override
	public final Throwable getCompletionException (final CallbackCompletion<?> completion) {
		Preconditions.checkArgument (completion == this.completion);
		this.enforceCompleted ();
		return (this.exception.get ());
	}
	
	@Override
	public final _Outcome_ getCompletionOutcome (final CallbackCompletion<?> completion) {
		Preconditions.checkArgument (completion == this.completion);
		this.enforceCompleted ();
		if (this.exception.get () != null)
			throw (new IllegalStateException ());
		return (this.outcome.get ());
	}
	
	@Override
	public final CallbackReactor getReactor () {
		return (null);
	}
	
	@Override
	public final void observeCompletion (final CallbackCompletion<?> completion, final CallbackCompletionObserver observer) {
		Preconditions.checkArgument (completion == this.completion);
		Preconditions.checkNotNull (observer);
		final boolean trigger;
		synchronized (this.pending) {
			if (this.pending.getCount () != 0) {
				this.observers.add (observer);
				trigger = false;
			} else
				trigger = true;
		}
		if (trigger)
			this.triggerObserver (observer);
	}
	
	final void enforceCompleted () {
		if (this.pending.getCount () != 0)
			throw (new IllegalStateException ());
		while (true) {
			final CallbackCompletionObserver observer = this.observers.poll ();
			if (observer == null)
				break;
			this.triggerObserver (observer);
		}
	}
	
	final CallbackCompletion<Void> triggerFailed (final Throwable exception) {
		synchronized (this.pending) {
			if (this.pending.getCount () != 1)
				throw (new IllegalStateException ());
			this.exception.set (exception);
			this.pending.countDown ();
		}
		this.enforceCompleted ();
		return (CallbackCompletion.createOutcome ());
	}
	
	final CallbackCompletion<Void> triggerSucceeded (final _Outcome_ outcome) {
		synchronized (this.pending) {
			if (this.pending.getCount () != 1)
				throw (new IllegalStateException ());
			this.outcome.set (outcome);
			this.pending.countDown ();
		}
		this.enforceCompleted ();
		return (CallbackCompletion.createOutcome ());
	}
	
	private final void triggerObserver (final CallbackCompletionObserver observer) {
		CallbackCompletion.triggerObserver (this.completion, observer);
	}
	
	public final CallbackCompletion<_Outcome_> completion;
	public final CallbackCompletionTrigger<_Outcome_> trigger;
	private final AtomicReference<Throwable> exception;
	private final LinkedBlockingQueue<CallbackCompletionObserver> observers;
	private final AtomicReference<_Outcome_> outcome;
	private final CountDownLatch pending;
	
	public static final <_Outcome_ extends Object> CallbackCompletionTrigger<_Outcome_> createTrigger () {
		return (new CallbackCompletionTriggerBackend<_Outcome_> ().trigger);
	}
}
