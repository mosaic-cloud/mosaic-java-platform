/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.implementation.container;


import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import eu.mosaic_cloud.cloudlets.implementation.container.CloudletComponentFsm.FsmState;
import eu.mosaic_cloud.cloudlets.implementation.container.CloudletComponentFsm.FsmTransition;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;


final class CloudletComponentFsm
		extends StateMachine<FsmState, FsmTransition>
{
	protected CloudletComponentFsm (final CloudletComponent component)
	{
		super (FsmState.class, FsmTransition.class);
		this.defineStates (FsmState.class);
		this.defineTransition (FsmTransition.CreateCompleted, FsmState.CreatePending, FsmState.RegisterPending2);
		this.defineTransition (FsmTransition.RegisterCompleted, new FsmState[] {FsmState.RegisterPending2, FsmState.RegisterPending1}, new FsmState[] {FsmState.RegisterPending1, FsmState.InitializePending});
		this.defineTransition (FsmTransition.InitializeCompleted, FsmState.InitializePending, FsmState.Active);
		this.defineTransition (FsmTransition.ExternalDestroy, FsmState.Active, FsmState.UnregisterPending2);
		this.defineTransition (FsmTransition.UnregisterCompleted, new FsmState[] {FsmState.UnregisterPending2, FsmState.UnregisterPending1}, new FsmState[] {FsmState.UnregisterPending1, FsmState.Destroyed});
		this.defineTransition (FsmTransition.InternalFailure, FsmState.values (), new FsmState[] {FsmState.Failed});
		this.initialize (FsmState.CreatePending);
		this.component = component;
	}
	
	protected final CloudletComponent component;
	
	protected abstract class FsmAccess<Input extends Object, Output extends Object>
			implements
				StateMachine.AccessorOperation<Accessor, Input, Output>
	{
		protected FsmAccess ()
		{
			super ();
		}
		
		@Override
		public final Output execute (final Accessor access, final Input input)
		{
			return (this.execute (input));
		}
		
		protected abstract Output execute (Input input);
		
		protected final Output trigger (final Input input)
		{
			return (CloudletComponentFsm.this.execute (this, input));
		}
	}
	
	protected abstract class FsmCallbackAccess
			extends FsmAccess<Void, CallbackCompletion<Void>>
	{
		protected FsmCallbackAccess ()
		{
			super ();
		}
		
		protected abstract CallbackCompletion<Void> execute ();
		
		@Override
		protected final CallbackCompletion<Void> execute (final Void input)
		{
			return (this.execute ());
		}
		
		protected final CallbackCompletion<Void> trigger ()
		{
			return (this.trigger (null));
		}
	}
	
	protected abstract class FsmCallbackCompletionTransaction
			extends FsmTransaction<CallbackCompletion<Void>, Void>
	{
		protected FsmCallbackCompletionTransaction (final FsmTransition transition)
		{
			super (transition);
		}
		
		protected final void observe (final CallbackCompletion<Void> completion)
		{
			completion.observe (new Observer (completion));
		}
		
		protected final class Observer
				extends Object
				implements
					CallbackCompletionObserver,
					CallbackProxy,
					Runnable
		{
			protected Observer (final CallbackCompletion<Void> completion)
			{
				super ();
				this.completion = completion;
			}
			
			@Override
			public final CallbackCompletion<Void> completed (final CallbackCompletion<?> completion1)
			{
				Preconditions.checkState (this.completion == completion1);
				return (CloudletComponentFsm.this.component.isolate.enqueue (this));
			}
			
			@Override
			public final void run ()
			{
				FsmCallbackCompletionTransaction.this.trigger (this.completion);
			}
			
			protected final CallbackCompletion<Void> completion;
		}
	}
	
	protected abstract class FsmCallbackTransaction
			extends FsmTransaction<Void, CallbackCompletion<Void>>
	{
		protected FsmCallbackTransaction (final FsmTransition transition)
		{
			super (transition);
		}
		
		protected abstract StateAndOutput<FsmState, CallbackCompletion<Void>> execute ();
		
		@Override
		protected final StateAndOutput<FsmState, CallbackCompletion<Void>> execute (final Void input)
		{
			return (this.execute ());
		}
		
		protected final CallbackCompletion<Void> trigger ()
		{
			return (this.trigger (null));
		}
	}
	
	protected abstract class FsmFutureCompletionAccess<Outcome>
			extends FsmAccess<Future<Outcome>, Void>
	{
		protected FsmFutureCompletionAccess ()
		{
			super ();
		}
		
		protected final void observe (final ListenableFuture<Outcome> completion)
		{
			final Observer observer = new Observer (completion);
			completion.addListener (observer, observer);
		}
		
		protected final class Observer
				extends Object
				implements
					Executor,
					Runnable
		{
			protected Observer (final Future<Outcome> completion)
			{
				super ();
				this.completion = completion;
			}
			
			@Override
			public final void execute (final Runnable runnable)
			{
				Preconditions.checkArgument (runnable == this);
				CloudletComponentFsm.this.component.isolate.enqueue (runnable);
			}
			
			@Override
			public final void run ()
			{
				FsmFutureCompletionAccess.this.trigger (this.completion);
			}
			
			protected final Future<Outcome> completion;
		}
	}
	
	protected abstract class FsmFutureCompletionTransaction<Outcome>
			extends FsmTransaction<Future<Outcome>, Void>
	{
		protected FsmFutureCompletionTransaction (final FsmTransition transition)
		{
			super (transition);
		}
		
		protected final void observe (final ListenableFuture<Outcome> completion)
		{
			final Observer observer = new Observer (completion);
			completion.addListener (observer, observer);
		}
		
		protected final class Observer
				extends Object
				implements
					Executor,
					Runnable
		{
			protected Observer (final Future<Outcome> completion)
			{
				super ();
				this.completion = completion;
			}
			
			@Override
			public final void execute (final Runnable runnable)
			{
				Preconditions.checkArgument (runnable == this);
				CloudletComponentFsm.this.component.isolate.enqueue (runnable);
			}
			
			@Override
			public final void run ()
			{
				FsmFutureCompletionTransaction.this.trigger (this.completion);
			}
			
			protected final Future<Outcome> completion;
		}
	}
	
	protected enum FsmState
			implements
				StateMachine.State
	{
		Active,
		CreatePending,
		Destroyed,
		Failed,
		InitializePending,
		RegisterPending1,
		RegisterPending2,
		UnregisterPending1,
		UnregisterPending2;
	}
	
	protected abstract class FsmTransaction<Input, Output>
			implements
				StateMachine.TransactionOperation<Transaction, FsmState, Input, Output>
	{
		protected FsmTransaction (final FsmTransition transition)
		{
			super ();
			this.transition = transition;
		}
		
		@Override
		public final StateAndOutput<FsmState, Output> execute (final Transaction transaction, final Input input)
		{
			return (this.execute (input));
		}
		
		protected abstract StateAndOutput<FsmState, Output> execute (Input input);
		
		protected final Output trigger (final Input input)
		{
			return CloudletComponentFsm.this.execute (this.transition, this, input);
		}
		
		protected final FsmTransition transition;
	}
	
	protected enum FsmTransition
			implements
				StateMachine.Transition
	{
		CreateCompleted,
		ExternalDestroy,
		InitializeCompleted,
		InternalFailure,
		RegisterCompleted,
		UnregisterCompleted;
	}
	
	protected abstract class FsmVoidAccess
			extends FsmAccess<Void, Void>
	{
		protected FsmVoidAccess ()
		{
			super ();
		}
		
		protected abstract Void execute ();
		
		@Override
		protected final Void execute (final Void input)
		{
			return (this.execute ());
		}
		
		protected final Void trigger ()
		{
			return (this.trigger (null));
		}
	}
	
	protected abstract class FsmVoidTransaction
			extends FsmTransaction<Void, Void>
	{
		protected FsmVoidTransaction (final FsmTransition transition)
		{
			super (transition);
		}
		
		protected abstract StateAndOutput<FsmState, Void> execute ();
		
		@Override
		protected final StateAndOutput<FsmState, Void> execute (final Void input)
		{
			return (this.execute ());
		}
		
		protected final Void trigger ()
		{
			return (this.trigger (null));
		}
	}
}
