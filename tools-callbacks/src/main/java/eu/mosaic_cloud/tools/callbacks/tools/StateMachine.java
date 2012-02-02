
package eu.mosaic_cloud.tools.callbacks.tools;


import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.DeferredException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.transcript.core.Transcript;


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
		this.capsule = new Capsule (stateClass, transitionClass, (transcript != null) ? transcript : Transcript.create (this), (exceptions != null) ? exceptions : ExceptionTracer.defaultInstance.get ());
		this.transcript = this.capsule.transcript;
		this.exceptions = this.capsule.exceptions;
		this.transcript.traceDebugging ("created machine `%{object}`.", this);
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
	
	protected final void defineTransition (final _Transition_ transition, final _State_ initialState, final _State_ ... finalStates)
	{
		synchronized (this.capsule.monitor) {
			Preconditions.checkState (this.capsule.currentState.get () == null);
			final TransitionDefinition definition = new TransitionDefinition (transition, initialState, finalStates);
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
	
	protected final ExceptionTracer exceptions;
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
				StateMachine.this.capsule.transcript.traceDebugging ("began machine `%{object}` access `%{object:identity}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
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
		
		public final void execute (final AccessorOperation<? super Accessor> operation)
				throws CaughtException
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` access `%{object:identity}` operation `%s`...", StateMachine.this, this, operation);
			try {
				this.begin ();
				try {
					operation.execute (this);
				} catch (final Throwable exception) {
					throw (new DeferredException (exception, "operation failed; aborting!"));
				}
			} finally {
				this.close ();
			}
		}
		
		public final void release ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (this, null));
				StateMachine.this.capsule.transcript.traceDebugging ("released machine `%{object}` access `%{object:identity}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
	}
	
	public static interface AccessorOperation<_Accessor_ extends StateMachine<?, ?>.Accessor>
	{
		public abstract void execute (final _Accessor_ accessor)
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
	
	public static interface State
	{}
	
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
		}
		
		public final Transaction begin ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentState.get () != null);
				Preconditions.checkState (StateMachine.this.capsule.currentState.get () == this.transitionDefinition.initialState);
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (null, this));
				StateMachine.this.capsule.transcript.traceDebugging ("began machine `%{object}` transaction `%{object:identity}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
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
				Preconditions.checkState (StateMachine.this.capsule.currentState.compareAndSet (this.transitionDefinition.initialState, finalState));
				StateMachine.this.capsule.transcript.traceDebugging ("commited machine `%{object}` transaction `%{object:identity}` with state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
		
		public final void execute (final TransactionOperation<_State_, ? super Transaction> operation)
				throws CaughtException
		{
			Preconditions.checkNotNull (operation);
			StateMachine.this.capsule.transcript.traceDebugging ("executing machine `%{object}` transaction `%{object:identity}` operation `%s`...", StateMachine.this, this, operation);
			try {
				this.begin ();
				_State_ state;
				try {
					state = operation.execute (this);
				} catch (final Throwable exception) {
					throw (new DeferredException (exception, "operation failed; aborting!"));
				}
				if (state != null)
					this.commit (state);
				else
					this.rollback ();
			} finally {
				this.close ();
			}
		}
		
		public final _Transition_ getTransition ()
		{
			return (this.transition);
		}
		
		public final void rollback ()
		{
			synchronized (StateMachine.this.capsule.monitor) {
				Preconditions.checkState (StateMachine.this.capsule.currentIsolate.compareAndSet (this, null));
				StateMachine.this.capsule.transcript.traceDebugging ("rollbacked machine `%{object}` transaction `%{object:identity}` in state `%s`...", StateMachine.this, this, StateMachine.this.capsule.currentState.get ().name ());
			}
		}
		
		protected final _Transition_ transition;
		final TransitionDefinition transitionDefinition;
	}
	
	public static interface TransactionOperation<_State_ extends Enum<_State_> & State, _Transaction_ extends StateMachine<_State_, ?>.Transaction>
	{
		public abstract _State_ execute (final _Transaction_ transaction)
				throws Throwable;
	}
	
	public static interface Transition
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
			this.exceptions = exceptions;
			this.currentState = Atomics.newReference (null);
			this.currentIsolate = Atomics.newReference (null);
		}
		
		final AtomicReference<Isolate> currentIsolate;
		final AtomicReference<_State_> currentState;
		final ExceptionTracer exceptions;
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
		TransitionDefinition (final _Transition_ transition, final _State_ initialState, final _State_[] finalStates_)
		{
			super ();
			Preconditions.checkNotNull (transition);
			Preconditions.checkArgument (StateMachine.this.capsule.transitionClass.isInstance (transition));
			Preconditions.checkNotNull (initialState);
			Preconditions.checkArgument (StateMachine.this.capsule.stateClass.isInstance (initialState));
			Preconditions.checkNotNull (finalStates_);
			final _State_[] finalStates = finalStates_.clone ();
			for (final _State_ state : finalStates) {
				Preconditions.checkNotNull (state);
				Preconditions.checkArgument (StateMachine.this.capsule.stateClass.isInstance (state));
			}
			this.transition = transition;
			this.initialState = initialState;
			this.finalStates = finalStates;
			this.ordinal = this.transition.ordinal ();
		}
		
		final _State_[] finalStates;
		final _State_ initialState;
		final int ordinal;
		final _Transition_ transition;
	}
}
