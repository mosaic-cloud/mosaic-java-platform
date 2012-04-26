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


import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.DeferredException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public class StateMachine<_State_ extends Enum<_State_> & StateMachine.State, _Transition_ extends Enum<_Transition_> & StateMachine.Transition>
		extends Object
{
	protected StateMachine (final Class<_State_> stateClass, final Class<_Transition_> transitionClass)
	{
		this (stateClass, transitionClass, null, null);
	}
	
	protected StateMachine (final Class<_State_> stateClass, final Class<_Transition_> transitionClass, final Transcript transcript, final ExceptionTracer exceptions)
	{
		super ();
		// FIXME: Refactor code to wrap the exception handler into a transcript exception handler.
		this.capsule = new Capsule (stateClass, transitionClass, (transcript != null) ? transcript : Transcript.create (this, true), (exceptions != null) ? exceptions : FallbackExceptionTracer.defaultInstance);
		this.transcript = this.capsule.transcript;
		this.exceptions = this.capsule.exceptions;
		this.transcript.traceDebugging ("created machine `%{object}`.", this);
	}
	
	public final void execute (final _Transition_ transition, final _State_ finalState)
			throws CaughtException.Wrapper
	{
		new Transaction (transition).execute_ (new Callable<StateAndOutput<_State_, Void>> () {
			@Override
			public final StateAndOutput<_State_, Void> call ()
			{
				return (StateAndOutput.create (finalState, null));
			}
		});
	}
	
	public final <_Output_ extends Object> _Output_ execute (final _Transition_ transition, final _State_ finalState, final Callable<_Output_> operation)
			throws CaughtException.Wrapper
	{
		return (new Transaction (transition).execute (finalState, operation));
	}
	
	public final void execute (final _Transition_ transition, final _State_ finalState, final Runnable operation)
			throws CaughtException.Wrapper
	{
		new Transaction (transition).execute (finalState, operation);
	}
	
	public final void execute (final _Transition_ transition, final Callable<_State_> operation)
			throws CaughtException.Wrapper
	{
		new Transaction (transition).execute (operation);
	}
	
	public final <_Input_ extends Object, _Output_ extends Object> _Output_ execute (final _Transition_ transition, final TransactionOperation<? super Transaction, _State_, _Input_, _Output_> operation, final _Input_ input)
			throws CaughtException.Wrapper
	{
		return (new Transaction (transition).execute (operation, input));
	}
	
	public final <_Input_ extends Object, _Output_ extends Object> _Output_ execute (final AccessorOperation<? super Accessor, _Input_, _Output_> operation, final _Input_ input)
			throws CaughtException.Wrapper
	{
		return (new Accessor ().execute (operation, input));
	}
	
	public final <_Output_ extends Object> _Output_ execute (final Callable<_Output_> operation)
			throws CaughtException.Wrapper
	{
		return (new Accessor ().execute (operation));
	}
	
	public final void execute (final Runnable operation)
			throws CaughtException.Wrapper
	{
		new Accessor ().execute (operation);
	}
	
	public final <_Output_ extends Object> _Output_ execute1 (final _Transition_ transition, final Callable<StateAndOutput<_State_, _Output_>> operation)
			throws CaughtException.Wrapper
	{
		return (new Transaction (transition).execute1 (operation));
	}
	
	public final _State_ getState ()
	{
		return (this.capsule.currentState.get ());
	}
	
	public final boolean hasState (final _State_ state)
	{
		Preconditions.checkNotNull (state);
		return (this.capsule.currentState.get () == state);
	}
	
	protected final void defineState (final _State_ state)
	{
		synchronized (this.capsule.monitor) {
			Preconditions.checkState (this.capsule.currentState.get () == null);
			final StateDefinition definition = new StateDefinition (state);
			Preconditions.checkArgument (this.capsule.stateDefinitions[definition.state.ordinal ()] == null);
			this.capsule.stateDefinitions[definition.state.ordinal ()] = definition;
		}
	}
	
	protected final void defineStates (final Class<_State_> states)
	{
		synchronized (this.capsule.monitor) {
			Preconditions.checkState (this.capsule.currentState.get () == null);
			for (final _State_ state : states.getEnumConstants ()) {
				final StateDefinition definition = new StateDefinition (state);
				Preconditions.checkState (this.capsule.stateDefinitions[definition.ordinal] == null);
				this.capsule.stateDefinitions[definition.ordinal] = definition;
			}
		}
	}
	
	protected final void defineTransition (final _Transition_ transition, final _State_ initialState, final _State_ finalState)
	{
		this.defineTransition (transition, (_State_[]) new Enum<?>[] {initialState}, (_State_[]) new Enum<?>[] {finalState});
	}
	
	protected final void defineTransition (final _Transition_ transition, final _State_ initialState, final _State_[] finalStates)
	{
		this.defineTransition (transition, (_State_[]) new Enum<?>[] {initialState}, finalStates);
	}
	
	protected final void defineTransition (final _Transition_ transition, final _State_[] initialStates, final _State_[] finalStates)
	{
		synchronized (this.capsule.monitor) {
			Preconditions.checkState (this.capsule.currentState.get () == null);
			final TransitionDefinition definition = new TransitionDefinition (transition, initialStates, finalStates);
			Preconditions.checkState (this.capsule.transitionDefinitions[definition.ordinal] == null);
			this.capsule.transitionDefinitions[definition.ordinal] = definition;
		}
	}
	
	protected final void initialize (final _State_ state)
	{
		synchronized (this.capsule.monitor) {
			Preconditions.checkArgument (state != null);
			Preconditions.checkState (this.capsule.currentState.get () == null);
			for (final StateDefinition definition : this.capsule.stateDefinitions)
				Preconditions.checkState (definition != null);
			for (final TransitionDefinition definition : this.capsule.transitionDefinitions)
				Preconditions.checkState (definition != null);
			Preconditions.checkState (this.capsule.currentState.compareAndSet (null, state));
			this.transcript.traceDebugging ("initialized machine `%{object}` with state `%s`.", this, state.name ());
		}
	}
	
	protected final TranscriptExceptionTracer exceptions;
	protected final Transcript transcript;
	final Capsule capsule;
	
	public class Accessor
			extends Isolate
	{
		public Accessor ()
		{
			super ();
		}
		
		public final Accessor begin ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentState.get () != null);
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (null, this));
				StateMachine.this.capsule.transcript.traceDebugging ("began machine `%{object}` access `%{object}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
				return (this);
			}
		}
		
		@Override
		@Deprecated
		public final void close ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				if (StateMachine.this.capsule.currentIsolate.get () == this)
					this.release ();
			}
		}
		
		public final <_Input_ extends Object, _Output_ extends Object> _Output_ execute (final AccessorOperation<? super Accessor, _Input_, _Output_> operation, final _Input_ input)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` access `%{object:identity}` operation `%{object}` in state `%s`...", StateMachine.this, this, operation, StateMachine.this.capsule.currentState.get ().name ());
			final _Output_ output = this.execute_ (new Callable<_Output_> () {
				@Override
				public final _Output_ call ()
				{
					try {
						return (operation.execute (Accessor.this, input));
					} catch (final CaughtException.Wrapper exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
					}
				}
			});
			return (output);
		}
		
		public final <_Output_ extends Object> _Output_ execute (final Callable<_Output_> operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` access `%{object:identity}` operation `%{object}` in state `%s`...", StateMachine.this, this, operation, StateMachine.this.capsule.currentState.get ().name ());
			final _Output_ output = this.execute_ (operation);
			return (output);
		}
		
		public final void execute (final Runnable operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` access `%{object:identity}` operation `%{object}` in state `%s`...", StateMachine.this, this, operation, StateMachine.this.capsule.currentState.get ().name ());
			this.execute_ (new Callable<Void> () {
				@Override
				public final Void call ()
				{
					operation.run ();
					return (null);
				}
			});
		}
		
		public final void release ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (this, null));
				StateMachine.this.capsule.transcript.traceDebugging ("released machine `%{object}` access `%{object}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
		
		private final <_Output_ extends Object> _Output_ execute_ (final Callable<_Output_> operation)
				throws CaughtException.Wrapper
		{
			try {
				this.begin ();
				final _Output_ output;
				try {
					output = operation.call ();
				} catch (final CaughtException.Wrapper exception) {
					throw (exception);
				} catch (final Throwable exception) {
					throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
				}
				return (output);
			} finally {
				this.close ();
			}
		}
	}
	
	public interface AccessorOperation<_Accessor_ extends StateMachine<?, ?>.Accessor, _Input_ extends Object, _Output_ extends Object>
	{
		public abstract _Output_ execute (final _Accessor_ accessor, _Input_ input)
				throws Throwable;
	}
	
	public abstract class Isolate
			extends Object
			implements
				AutoCloseable
	{
		Isolate ()
		{
			super ();
			this.capsule = StateMachine.this.capsule;
		}
		
		public final void enforceAnyState (final _State_ state1, final _State_ state2)
		{
			this.enforceAnyStates (state1, state2);
		}
		
		public final void enforceAnyState (final _State_ state1, final _State_ state2, final _State_ state3)
		{
			this.enforceAnyStates (state1, state2, state3);
		}
		
		@SafeVarargs
		public final void enforceAnyStates (final _State_ ... states)
		{
			Preconditions.checkState (this.hasAnyStates (states));
		}
		
		public final void enforceState (final _State_ state)
		{
			this.enforceAnyStates (state);
		}
		
		public final _State_ getState ()
		{
			synchronized (this.capsule.monitor) {
				final _State_ currentState = this.capsule.currentState.get ();
				Preconditions.checkState (currentState != null);
				return (currentState);
			}
		}
		
		public final boolean hasAnyState (final _State_ state1, final _State_ state2)
		{
			return (this.hasAnyStates (state1, state2));
		}
		
		public final boolean hasAnyState (final _State_ state1, final _State_ state2, final _State_ state3)
		{
			return (this.hasAnyStates (state1, state2, state3));
		}
		
		@SafeVarargs
		public final boolean hasAnyStates (final _State_ ... states)
		{
			synchronized (this.capsule.monitor) {
				Preconditions.checkNotNull (states);
				final _State_ currentState = this.capsule.currentState.get ();
				Preconditions.checkState (currentState != null);
				for (final _State_ state : states) {
					Preconditions.checkNotNull (state);
					if (currentState == state)
						return (true);
				}
				return (false);
			}
		}
		
		public final boolean hasState (final _State_ state)
		{
			return (this.hasAnyStates (state));
		}
		
		final Capsule capsule;
	}
	
	public interface State
	{}
	
	public static final class StateAndOutput<_State_ extends Enum<_State_> & State, _Output_ extends Object>
			extends Object
	{
		private StateAndOutput (final _State_ state, final _Output_ output)
		{
			super ();
			this.state = state;
			this.output = output;
		}
		
		public static final <_State_ extends Enum<_State_> & State, _Output_ extends Object> StateAndOutput<_State_, _Output_> create (final _State_ state, final _Output_ output)
		{
			return (new StateAndOutput<_State_, _Output_> (state, output));
		}
		
		public final _Output_ output;
		public final _State_ state;
	}
	
	public class Transaction
			extends Isolate
	{
		public Transaction (final _Transition_ transition)
		{
			super ();
			Preconditions.checkNotNull (transition);
			Preconditions.checkArgument (StateMachine.this.capsule.transitionClass.isInstance (transition));
			this.transition = transition;
			this.transitionDefinition = StateMachine.this.capsule.transitionDefinitions[transition.ordinal ()];
			Preconditions.checkState (this.transitionDefinition != null);
			this.initialState = null;
		}
		
		public final Transaction begin ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentState.get () != null);
				final _State_ initialState = StateMachine.this.capsule.currentState.get ();
				initialStateValid : while (true) {
					for (final _State_ state : this.transitionDefinition.initialStates)
						if (state == initialState)
							break initialStateValid;
					throw (new IllegalStateException ());
				}
				this.initialState = initialState;
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (null, this));
				StateMachine.this.capsule.transcript.traceDebugging ("began machine `%{object}` transaction `%{object}` with transition `%s` in state `%s`...", StateMachine.this, this, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
				return (this);
			}
		}
		
		@Override
		@Deprecated
		public final void close ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				if (StateMachine.this.capsule.currentIsolate.get () == this)
					this.rollback ();
			}
		}
		
		public final void commit (final _State_ finalState)
		{
			synchronized (StateMachine.this.capsule.monitor) {
				finalStateValid : while (true) {
					for (final _State_ state : this.transitionDefinition.finalStates)
						if (state == finalState)
							break finalStateValid;
					throw (new IllegalStateException ());
				}
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (this, null));
				Preconditions.checkState (StateMachine.this.capsule.currentState.compareAndSet (this.initialState, finalState));
				StateMachine.this.capsule.transcript.traceDebugging ("commited machine `%{object}` transaction `%{object}` with state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
		
		public final <_Output_ extends Object> _Output_ execute (final _State_ finalState, final Callable<_Output_> operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (finalState);
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%{object}` with transition `%s` in state `%s`...", StateMachine.this, this, operation, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
			final _Output_ output = this.execute_ (new Callable<StateAndOutput<_State_, _Output_>> () {
				@Override
				public final StateAndOutput<_State_, _Output_> call ()
				{
					final _Output_ output;
					try {
						output = operation.call ();
					} catch (final CaughtException.Wrapper exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
					}
					return (StateAndOutput.create (finalState, output));
				}
			});
			return (output);
		}
		
		public final void execute (final _State_ finalState, final Runnable operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (finalState);
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%{object}` with transition `%s` in state `%s`...", StateMachine.this, this, operation, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
			this.execute_ (new Callable<StateAndOutput<_State_, Void>> () {
				@Override
				public final StateAndOutput<_State_, Void> call ()
				{
					try {
						operation.run ();
					} catch (final CaughtException.Wrapper exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
					}
					return (StateAndOutput.create (finalState, null));
				}
			});
		}
		
		public final void execute (final Callable<_State_> operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%{object}` with transition `%s` in state `%s`...", StateMachine.this, this, operation, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
			this.execute_ (new Callable<StateAndOutput<_State_, Void>> () {
				@Override
				public final StateAndOutput<_State_, Void> call ()
				{
					final _State_ finalState;
					try {
						finalState = operation.call ();
					} catch (final CaughtException.Wrapper exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
					}
					return (StateAndOutput.create (finalState, null));
				}
			});
		}
		
		public final <_Input_ extends Object, _Output_ extends Object> _Output_ execute (final TransactionOperation<? super Transaction, _State_, _Input_, _Output_> operation, final _Input_ input)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%{object}` with transaction `%s` in state `%s`...", StateMachine.this, this, operation, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
			final _Output_ output = this.execute_ (new Callable<StateAndOutput<_State_, _Output_>> () {
				@Override
				public final StateAndOutput<_State_, _Output_> call ()
				{
					try {
						return (operation.execute (Transaction.this, input));
					} catch (final CaughtException.Wrapper exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
					}
				}
			});
			return (output);
		}
		
		public final <_Output_ extends Object> _Output_ execute1 (final Callable<StateAndOutput<_State_, _Output_>> operation)
				throws CaughtException.Wrapper
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%{object}` with transition `%s` in state `%s`...", StateMachine.this, this, operation, this.transitionDefinition.transition.name (), StateMachine.this.capsule.currentState.get ().name ());
			final _Output_ output = this.execute_ (operation);
			return (output);
		}
		
		public final _Transition_ getTransition ()
		{
			return (this.transition);
		}
		
		public final void rollback ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (this, null));
				StateMachine.this.capsule.transcript.traceDebugging ("rollbacked machine `%{object}` transaction `%{object}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
		
		private final <_Output_ extends Object> _Output_ execute_ (final Callable<StateAndOutput<_State_, _Output_>> operation)
				throws CaughtException.Wrapper
		{
			try {
				this.begin ();
				final StateAndOutput<_State_, _Output_> stateAndOutput;
				try {
					stateAndOutput = operation.call ();
				} catch (final CaughtException.Wrapper exception) {
					throw (exception);
				} catch (final Throwable exception) {
					throw (new DeferredException (exception, "operation failed; aborting!").wrap ());
				}
				final _State_ state;
				final _Output_ output;
				if (stateAndOutput != null) {
					state = stateAndOutput.state;
					output = stateAndOutput.output;
				} else {
					state = null;
					output = null;
				}
				if (state != null)
					this.commit (stateAndOutput.state);
				else
					this.rollback ();
				return (output);
			} finally {
				this.close ();
			}
		}
		
		protected final _Transition_ transition;
		final TransitionDefinition transitionDefinition;
		private _State_ initialState;
	}
	
	public interface TransactionOperation<_Transaction_ extends StateMachine<_State_, ?>.Transaction, _State_ extends Enum<_State_> & State, _Input_ extends Object, _Output_ extends Object>
	{
		public abstract StateAndOutput<_State_, _Output_> execute (final _Transaction_ transaction, _Input_ input)
				throws Throwable;
	}
	
	public interface Transition
	{}
	
	final class Capsule
			extends Object
	{
		Capsule (final Class<_State_> stateClass, final Class<_Transition_> transitionClass, final Transcript transcript, final ExceptionTracer exceptions)
		{
			Preconditions.checkNotNull (stateClass);
			Preconditions.checkArgument (stateClass.isEnum ());
			Preconditions.checkNotNull (transitionClass);
			Preconditions.checkArgument (transitionClass.isEnum ());
			Preconditions.checkNotNull (transcript);
			Preconditions.checkNotNull (exceptions);
			this.monitor = Monitor.create (StateMachine.this);
			this.stateClass = stateClass;
			this.stateDefinitions = new StateMachine.StateDefinition[this.stateClass.getEnumConstants ().length];
			this.transitionClass = transitionClass;
			this.transitionDefinitions = new StateMachine.TransitionDefinition[this.transitionClass.getEnumConstants ().length];
			this.transcript = transcript;
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
			this.currentState = Atomics.newReference (null);
			this.currentIsolate = Atomics.newReference (null);
		}
		
		final AtomicReference<Isolate> currentIsolate;
		final AtomicReference<_State_> currentState;
		final TranscriptExceptionTracer exceptions;
		final Monitor monitor;
		final Class<_State_> stateClass;
		final StateDefinition[] stateDefinitions;
		final Transcript transcript;
		final Class<_Transition_> transitionClass;
		final TransitionDefinition[] transitionDefinitions;
	}
	
	final class StateDefinition
			extends Object
			implements
				State
	{
		StateDefinition (final _State_ state)
		{
			super ();
			Preconditions.checkNotNull (state);
			Preconditions.checkArgument (StateMachine.this.capsule.stateClass.isInstance (state));
			this.state = state;
			this.ordinal = this.state.ordinal ();
		}
		
		final int ordinal;
		final _State_ state;
	}
	
	final class TransitionDefinition
			extends Object
			implements
				Transition
	{
		TransitionDefinition (final _Transition_ transition, final _State_[] initialStates_, final _State_[] finalStates_)
		{
			super ();
			Preconditions.checkNotNull (transition);
			Preconditions.checkArgument (StateMachine.this.capsule.transitionClass.isInstance (transition));
			Preconditions.checkNotNull (initialStates_);
			Preconditions.checkNotNull (finalStates_);
			final _State_[] initialStates = initialStates_.clone ();
			for (final _State_ state : initialStates) {
				Preconditions.checkNotNull (state);
				Preconditions.checkArgument (StateMachine.this.capsule.stateClass.isInstance (state));
			}
			final _State_[] finalStates = finalStates_.clone ();
			for (final _State_ state : finalStates) {
				Preconditions.checkNotNull (state);
				Preconditions.checkArgument (StateMachine.this.capsule.stateClass.isInstance (state));
			}
			this.transition = transition;
			this.initialStates = initialStates;
			this.finalStates = finalStates;
			this.ordinal = this.transition.ordinal ();
		}
		
		final _State_[] finalStates;
		final _State_[] initialStates;
		final int ordinal;
		final _Transition_ transition;
	}
}
