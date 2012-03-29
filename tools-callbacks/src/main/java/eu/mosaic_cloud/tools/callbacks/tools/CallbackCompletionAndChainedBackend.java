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

package eu.mosaic_cloud.tools.callbacks.tools;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionBackend;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.tools.QueuedExceptions;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class CallbackCompletionAndChainedBackend
		implements
			CallbackCompletionBackend
{
	private CallbackCompletionAndChainedBackend (final CallbackCompletion<?>[] dependents)
	{
		super ();
		Preconditions.checkNotNull (dependents);
		this.pending = new CountDownLatch (dependents.length);
		this.outcome = Atomics.newReference ();
		this.exceptions = new LinkedBlockingQueue<CaughtException> ();
		this.exception = QueuedExceptions.create (this.exceptions);
		this.observers = new LinkedBlockingQueue<CallbackCompletionObserver> ();
		this.completion = CallbackCompletion.createDeferred (this);
		for (final CallbackCompletion<?> dependent : dependents) {
			Preconditions.checkNotNull (dependent);
			dependent.observe (new Observer (dependent));
		}
	}
	
	@Override
	public final boolean awaitCompletion (final CallbackCompletion<?> completion, final long timeout)
	{
		Preconditions.checkArgument (completion == this.completion);
		return (Threading.await (this.pending, timeout));
	}
	
	@Override
	public final Throwable getCompletionException (final CallbackCompletion<?> completion)
	{
		Preconditions.checkArgument (completion == this.completion);
		this.enforceCompleted ();
		if (!this.outcome.get ().booleanValue ())
			return (this.exception);
		return (null);
	}
	
	@Override
	public final Object getCompletionOutcome (final CallbackCompletion<?> completion)
	{
		Preconditions.checkArgument (completion == this.completion);
		this.enforceCompleted ();
		if (!this.outcome.get ().booleanValue ())
			throw (new IllegalStateException ());
		return (null);
	}
	
	@Override
	public final CallbackReactor getReactor ()
	{
		return (null);
	}
	
	@Override
	public final void observeCompletion (final CallbackCompletion<?> completion, final CallbackCompletionObserver observer)
	{
		Preconditions.checkArgument (completion == this.completion);
		Preconditions.checkNotNull (observer);
		final boolean trigger;
		synchronized (this.pending) {
			if (this.pending.getCount () > 0) {
				this.observers.add (observer);
				trigger = false;
			} else
				trigger = true;
		}
		if (trigger)
			this.triggerObserver (observer);
	}
	
	final void enforceCompleted ()
	{
		if (!Threading.await (this.pending, 0))
			throw (new IllegalStateException ());
		if (this.outcome.get () == null) {
			final boolean trigger;
			if (this.exceptions.isEmpty ())
				trigger = this.outcome.compareAndSet (null, Boolean.TRUE);
			else
				trigger = this.outcome.compareAndSet (null, Boolean.FALSE);
			if (trigger) {
				while (true) {
					final CallbackCompletionObserver observer = this.observers.poll ();
					if (observer == null)
						break;
					this.triggerObserver (observer);
				}
			}
		}
	}
	
	final CallbackCompletion<Void> triggerCompleted (final CallbackCompletion<?> completion)
	{
		final Throwable exception = completion.getException ();
		if (exception != null)
			this.exceptions.add (CaughtException.create (ExceptionResolution.Deferred, exception));
		synchronized (this.pending) {
			this.pending.countDown ();
			if (this.pending.getCount () == 0)
				this.enforceCompleted ();
		}
		return (CallbackCompletion.createOutcome ());
	}
	
	private final void triggerObserver (final CallbackCompletionObserver observer)
	{
		CallbackCompletion.triggerObserver (this.completion, observer);
	}
	
	public static final CallbackCompletion<Void> createCompletion (final CallbackCompletion<?>[] dependents)
	{
		return (new CallbackCompletionAndChainedBackend (dependents).completion);
	}
	
	public final CallbackCompletion<Void> completion;
	private final QueuedExceptions exception;
	private final LinkedBlockingQueue<CaughtException> exceptions;
	private final LinkedBlockingQueue<CallbackCompletionObserver> observers;
	private final AtomicReference<Boolean> outcome;
	private final CountDownLatch pending;
	
	private final class Observer
			extends Object
			implements
				CallbackCompletionObserver
	{
		Observer (final CallbackCompletion<?> dependent)
		{
			super ();
			this.dependent = dependent;
			this.triggered = new AtomicBoolean (false);
		}
		
		@Override
		public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion)
		{
			Preconditions.checkState (completion == this.dependent);
			Preconditions.checkState (this.triggered.compareAndSet (false, true));
			return (CallbackCompletionAndChainedBackend.this.triggerCompleted (this.dependent));
		}
		
		private final CallbackCompletion<?> dependent;
		private final AtomicBoolean triggered;
	}
}
