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

package eu.mosaic_cloud.tools.callbacks.implementations.basic;


import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCanceled;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;


public final class BasicCallbackReactor
		extends Object
		implements
			CallbackReactor
{
	BasicCallbackReactor (final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		super ();
		this.reactor = new Reactor (this, threading, exceptions);
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference assignDelegate (final _Callbacks_ proxy, final _Callbacks_ delegate)
	{
		return (this.reactor.triggerAssign (proxy, delegate));
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference assignHandler (final _Callbacks_ proxy, final CallbackHandler handler, final CallbackIsolate isolate)
	{
		return (this.reactor.triggerAssign (proxy, handler, isolate));
	}
	
	@Override
	public final boolean await ()
	{
		return (this.await (0));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		return (this.reactor.await (timeout));
	}
	
	@Override
	public final CallbackIsolate createIsolate ()
	{
		return (this.reactor.createIsolate ());
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> _Callbacks_ createProxy (final Class<_Callbacks_> specification)
	{
		return (this.reactor.createProxy (specification));
	}
	
	public final void destroy ()
	{
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout)
	{
		this.reactor.triggerDestroy ();
		return (this.reactor.await (timeout));
	}
	
	@Override
	public final CallbackReference destroyIsolate (final CallbackIsolate isolate)
	{
		return (this.reactor.triggerDestroyIsolate (isolate));
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference destroyProxy (final _Callbacks_ proxy)
	{
		return (this.reactor.triggerDestroyProxy (proxy));
	}
	
	public final void initialize ()
	{
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout)
	{
		Preconditions.checkArgument (timeout >= -1);
		return (true);
	}
	
	final Reactor reactor;
	
	public static final BasicCallbackReactor create (final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new BasicCallbackReactor (threading, exceptions));
	}
	
	public static final class Completion
			extends AbstractFuture<Boolean>
			implements
				CallbackCompletion
	{
		public Completion (final WeakReference<CallbackReactor> reactor)
		{
			super ();
			Preconditions.checkState (reactor != null);
			this.reference = CallbackReference.create (reactor, this);
		}
		
		Completion (final Reactor reactor)
		{
			super ();
			Preconditions.checkState (reactor != null);
			this.reference = CallbackReference.create (reactor.reference, this);
		}
		
		@Override
		public final boolean cancel (final boolean interrupt)
		{
			if (interrupt)
				throw (new UnsupportedOperationException ());
			return (false);
		}
		
		public final void triggerFailure (final Throwable exception)
		{
			this.setException (exception);
		}
		
		public final void triggerSuccess ()
		{
			this.set (Boolean.TRUE);
		}
		
		@Override
		protected final void interruptTask ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		public final CallbackReference reference;
	}
	
	static abstract class Action<_Target_ extends ActionTarget>
			extends Object
	{
		Action (final Reactor reactor, final _Target_ target, final Completion completion)
		{
			super ();
			this.reactor = reactor;
			this.target = target;
			this.completion = completion;
		}
		
		final Completion completion;
		final Reactor reactor;
		final _Target_ target;
	}
	
	interface ActionTarget
	{}
	
	static final class Actor<_Callbacks_ extends Callbacks>
			extends Object
			implements
				ActionTarget,
				InvocationHandler
	{
		Actor (final Reactor reactor, final Class<_Callbacks_> specification)
		{
			super ();
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.reactor = reactor;
				this.specification = specification;
				this.destroyCompletion = new Completion (this.reactor);
				this.proxy = (CallbackProxy) Proxy.newProxyInstance (specification.getClassLoader (), new Class[] {specification, CallbackProxy.class}, this);
				this.actions = new ConcurrentLinkedQueue<ActorAction> ();
				this.handler = Atomics.newReference (null);
				this.scheduler = Atomics.newReference (null);
				this.delegate = Atomics.newReference (null);
				this.failed = Atomics.newReference (null);
				this.assignAction = Atomics.newReference (null);
				this.destroyAction = Atomics.newReference (null);
				this.handlerStatus = Atomics.newReference (HandlerStatus.Unassigned);
				this.scheduleStatus = Atomics.newReference (ScheduleStatus.Idle);
				this.status = Atomics.newReference (Status.Active);
				this.reactor.registerActor (this);
				this.reactor.transcript.traceDebugging ("created proxy `%{object:identity}` (owned by actor `%{object:identity}` from reactor `%{object:identity}`).", this.proxy, this, this.reactor.facade);
			}
		}
		
		@Override
		public final Object invoke (final Object callbacks, final Method method, final Object[] arguments)
				throws Throwable
		{
			if (method.getDeclaringClass () == Object.class)
				return (method.invoke (this, arguments));
			if (method.getReturnType () != CallbackReference.class)
				throw (new IllegalAccessError ());
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("invocking (triggered) for proxy `%{object:identity}` the method `%{method}` with arguments `%{array}`...", this.proxy, method, arguments);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (this.handlerStatus.get () != HandlerStatus.Delegated) {
					final ActorCallbackAction action = new ActorCallbackAction (this, method, arguments);
					this.enqueueAction (action);
					return (action.completion.reference);
				}
			}
			return (this.delegateInvokeCallback (method, arguments, null));
		}
		
		final void delegateInvokeCallback (final ActorCallbackAction action)
		{
			this.delegateInvokeCallback (action.method, action.arguments, action.completion);
		}
		
		final CallbackReference delegateInvokeCallback (final Method method, final Object[] arguments, final Completion chainedCompletion)
		{
			final Object delegate = this.delegate.get ();
			Preconditions.checkState (delegate != null);
			this.reactor.transcript.traceDebugging ("invocking method callback on delegate `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) the method `%{method}` with arguments `%{array}`...", delegate, this.proxy, this, method, arguments);
			CallbackReference reference;
			try {
				reference = (CallbackReference) method.invoke (delegate, arguments);
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceDeferredException (exception);
				final Completion completion = new Completion (this.reactor);
				completion.triggerFailure (exception);
				this.triggerFailure (exception);
				reference = completion.reference;
			}
			Preconditions.checkState (reference != null);
			if (chainedCompletion != null) {
				final ListenableFuture<?> completion = reference.getCompletion ();
				completion.addListener (new Runnable () {
					@Override
					public final void run ()
					{
						try {
							Preconditions.checkState (completion.isDone ());
							completion.get (0, TimeUnit.MILLISECONDS);
							chainedCompletion.triggerSuccess ();
						} catch (final ExecutionException exception) {
							final Throwable cause = exception.getCause ();
							Actor.this.reactor.exceptions.traceDeferredException (cause);
							Actor.this.reactor.exceptions.traceHandledException (exception);
							chainedCompletion.triggerFailure (cause);
						} catch (final Throwable exception) {
							Actor.this.reactor.exceptions.traceDeferredException (exception);
							chainedCompletion.triggerFailure (exception);
						}
					}
				}, MoreExecutors.sameThreadExecutor ());
			}
			return (reference);
		}
		
		final void enqueueAction (final ActorAction action)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("enqueueing action `%{object}` on actor `%{object:identity}`...", action, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (action instanceof ActorCallbackAction) {
					Preconditions.checkState (this.actions.offer (action));
					this.schedule (false);
				} else if (action instanceof ActorAssignAction) {
					Preconditions.checkState (this.assignAction.compareAndSet (null, (ActorAssignAction) action));
					this.schedule (true);
				} else if (action instanceof ActorDestroyAction) {
					Preconditions.checkState (this.destroyAction.compareAndSet (null, (ActorDestroyAction) action));
					this.schedule (true);
				} else
					throw (new IllegalStateException ());
			}
		}
		
		final void executeActions ()
		{
			this.reactor.transcript.traceDebugging ("executing enqueued actions on actor `%{object:identity}`...", this);
			Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Scheduled, ScheduleStatus.Running));
			while (true) {
				boolean reschedule = false;
				if ((this.assignAction.get () != null) && (this.failed.get () == null)) {
					this.executeAssign ();
					reschedule |= true;
				}
				while (true) {
					if (this.failed.get () != null)
						break;
					final ActorAction action = this.actions.poll ();
					if (action == null)
						break;
					if (action instanceof ActorCallbackAction) {
						this.executeInvokeCallback ((ActorCallbackAction) action);
						reschedule |= true;
					} else
						throw (new IllegalStateException ());
				}
				if (this.destroyAction.get () != null) {
					this.executeDestroy ();
					reschedule |= true;
				}
				if (!reschedule)
					break;
			}
			Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.Idle));
			this.reactor.transcript.traceDebugging ("executed enqueued actions on actor `%{object:identity}`...", this);
		}
		
		final void executeAssign ()
		{
			synchronized (this.monitor) {
				final ActorAssignAction action = this.assignAction.get ();
				Preconditions.checkState (action != null);
				this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", action, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.handlerStatus.get () == HandlerStatus.Registering);
				final CallbackHandler handler = this.handler.get ();
				Preconditions.checkState ((handler != null) && (handler == action.handler));
				final Scheduler scheduler = this.scheduler.get ();
				Preconditions.checkState ((scheduler != null) && (scheduler == action.scheduler));
				this.reactor.transcript.traceDebugging ("assigning handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				this.reactor.transcript.traceDebugging ("invocking register callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				try {
					handler.registeredCallbacks (this.specification.cast (this.proxy), scheduler.isolate);
				} catch (final Throwable exception) {
					this.reactor.exceptions.traceDeferredException (exception);
					this.triggerFailure (exception);
					return;
				}
				action.completion.triggerSuccess ();
				Preconditions.checkState (this.assignAction.compareAndSet (action, null));
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Registering, HandlerStatus.Assigned));
				this.reactor.transcript.traceDebugging ("assigned handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`).", handler, this.proxy, this, scheduler.isolate, scheduler);
				this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`...", action, this);
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				final ActorDestroyAction destroy = this.destroyAction.get ();
				Preconditions.checkState (destroy != null);
				this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", destroy, this);
				Preconditions.checkState (this.status.get () == Status.Destroying);
				switch (this.handlerStatus.get ()) {
					case Unassigned :
					case Registering :
					case Assigned :
					case Delegated :
						break;
					default:
						throw (new IllegalStateException ());
				}
				this.reactor.transcript.traceDebugging ("destroying proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", this.proxy, this);
				final Throwable failure = this.failed.get ();
				if (failure == null) {
					final ActorAssignAction assign = this.assignAction.get ();
					final CallbackHandler handler = this.handler.get ();
					if ((handler != null) && (assign == null)) {
						final Scheduler scheduler = this.scheduler.get ();
						Preconditions.checkState (scheduler != null);
						this.reactor.transcript.traceDebugging ("invocking unregister callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
						try {
							handler.unregisteredCallbacks (this.specification.cast (this.proxy));
						} catch (final Throwable exception) {
							this.reactor.exceptions.traceDeferredException (exception);
							this.triggerFailure (exception);
							return;
						}
						scheduler.unregisterActor (this);
						Preconditions.checkState (this.handler.compareAndSet (handler, null));
						Preconditions.checkState (this.scheduler.compareAndSet (scheduler, null));
					} else {
						if (assign != null) {
							assign.completion.triggerFailure (new CallbackCanceled ());
							Preconditions.checkState (this.assignAction.compareAndSet (assign, null));
						}
						for (final ActorAction action : this.actions)
							action.completion.triggerFailure (new CallbackCanceled ());
						this.actions.clear ();
					}
				} else {
					final ActorAssignAction assign = this.assignAction.get ();
					final CallbackHandler handler = this.handler.get ();
					if (handler != null) {
						final Scheduler scheduler = this.scheduler.get ();
						Preconditions.checkState (scheduler != null);
						this.reactor.transcript.traceDebugging ("invocking failure callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
						try {
							handler.failedCallbacks (this.specification.cast (this.proxy), failure);
						} catch (final Throwable exception) {
							this.reactor.exceptions.traceIgnoredException (exception);
						}
						scheduler.unregisterActor (this);
						Preconditions.checkState (this.handler.compareAndSet (handler, null));
						Preconditions.checkState (this.scheduler.compareAndSet (scheduler, null));
					}
					if (assign != null) {
						assign.completion.triggerFailure (failure);
						Preconditions.checkState (this.assignAction.compareAndSet (assign, null));
					}
					for (final ActorAction action : this.actions)
						action.completion.triggerFailure (failure);
					this.actions.clear ();
				}
				Preconditions.checkState (this.assignAction.get () == null);
				Preconditions.checkState (this.handler.get () == null);
				Preconditions.checkState (this.scheduler.get () == null);
				this.reactor.unregisterActor (this);
				if (failure == null)
					destroy.completion.triggerSuccess ();
				else
					destroy.completion.triggerFailure (failure);
				Preconditions.checkState (this.destroyAction.compareAndSet (destroy, null));
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
				this.reactor.transcript.traceDebugging ("destroyed proxy `%{object:identity}` (owned by actor `%{object:identity}`).", this.proxy, this);
				this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`.", destroy, this);
			}
		}
		
		final void executeInvokeCallback (final ActorCallbackAction action)
		{
			Preconditions.checkState (action != null);
			this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", action, this);
			final CallbackHandler handler;
			final Scheduler scheduler;
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.handlerStatus.get () == HandlerStatus.Assigned);
				handler = this.handler.get ();
				Preconditions.checkState (handler != null);
				scheduler = this.scheduler.get ();
				Preconditions.checkState (scheduler != null);
			}
			this.reactor.transcript.traceDebugging ("invocking method callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`) the method `%{method}` with arguments `%{array}`...", handler, this.proxy, this, scheduler.isolate, scheduler, action.method, action.arguments);
			try {
				final Object outcome = action.method.invoke (handler, action.arguments);
				// !!!!
				// if (outcome != null)
				//	throw (new IllegalArgumentException ());
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceDeferredException (exception);
				this.triggerFailure (exception);
				return;
			}
			action.completion.triggerSuccess ();
			this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`...", action, this);
		}
		
		final void schedule (final boolean force)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (!force) {
					switch (this.handlerStatus.get ()) {
						case Assigned :
						case Registering :
							break;
						case Unassigned :
							return;
						default:
							throw (new IllegalStateException ());
					}
					if (this.handler.get () == null)
						return;
				}
				if ((this.scheduleStatus.get () == ScheduleStatus.Scheduled) || (this.scheduleStatus.get () == ScheduleStatus.Running))
					return;
				final Scheduler scheduler = this.scheduler.get ();
				Preconditions.checkNotNull (scheduler);
				Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Idle, ScheduleStatus.Scheduled));
				scheduler.enqueueActor (this);
				this.reactor.transcript.traceDebugging ("scheduled actor `%{object:identity}`.", this);
			}
		}
		
		final CallbackReference triggerAssign (final CallbackHandler handler, final Scheduler scheduler)
		{
			Preconditions.checkNotNull (scheduler);
			Preconditions.checkNotNull (handler);
			Preconditions.checkArgument (this.specification.isInstance (handler));
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("assigning (triggered) handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Unassigned, HandlerStatus.Registering));
				Preconditions.checkState (this.handler.compareAndSet (null, handler));
				Preconditions.checkState (this.scheduler.compareAndSet (null, scheduler));
				Preconditions.checkState (this.delegate.get () == null);
				scheduler.registerActor (this);
				final ActorAssignAction action = new ActorAssignAction (this, handler, scheduler);
				this.enqueueAction (action);
				return (action.completion.reference);
			}
		}
		
		final CallbackReference triggerAssign (final Callbacks delegate)
		{
			Preconditions.checkNotNull (delegate);
			Preconditions.checkArgument (this.specification.isInstance (delegate));
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("assigning (triggered) delegate `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", delegate, this.proxy, this);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Unassigned, HandlerStatus.Delegated));
				Preconditions.checkState (this.delegate.compareAndSet (null, (_Callbacks_) delegate));
				Preconditions.checkState (this.handler.get () == null);
				Preconditions.checkState (this.scheduler.get () == null);
				for (final Action<?> action : this.actions)
					if (action instanceof ActorCallbackAction)
						this.delegateInvokeCallback ((ActorCallbackAction) action);
					else
						throw (new IllegalStateException ());
				final Completion completion = new Completion (this.reactor);
				completion.triggerSuccess ();
				return (completion.reference);
			}
		}
		
		final CallbackReference triggerDestroy ()
		{
			this.reactor.transcript.traceDebugging ("destroying (triggered) proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", this.proxy, this);
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return (this.destroyCompletion.reference);
						default:
							throw (new IllegalStateException ());
					}
				}
				final ActorDestroyAction action = new ActorDestroyAction (this, this.destroyCompletion);
				switch (this.handlerStatus.get ()) {
					case Unassigned :
					case Delegated :
						Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
						this.executeDestroy ();
						break;
					case Registering :
					case Assigned :
						this.enqueueAction (action);
						break;
					default:
						throw (new IllegalStateException ());
				}
				return (action.completion.reference);
			}
		}
		
		final void triggerFailure (final Throwable exception)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.failed.compareAndSet (null, exception);
				this.triggerDestroy ();
			}
		}
		
		final ConcurrentLinkedQueue<ActorAction> actions;
		final AtomicReference<ActorAssignAction> assignAction;
		final AtomicReference<_Callbacks_> delegate;
		final AtomicReference<ActorDestroyAction> destroyAction;
		final Completion destroyCompletion;
		final AtomicReference<Throwable> failed;
		final AtomicReference<CallbackHandler> handler;
		final AtomicReference<HandlerStatus> handlerStatus;
		final Monitor monitor;
		final CallbackProxy proxy;
		final Reactor reactor;
		final AtomicReference<Scheduler> scheduler;
		final AtomicReference<ScheduleStatus> scheduleStatus;
		final Class<_Callbacks_> specification;
		final AtomicReference<Status> status;
		
		static enum HandlerStatus
		{
			Assigned (),
			Delegated (),
			Registering (),
			Unassigned ();
		}
		
		static enum ScheduleStatus
		{
			Idle (),
			Running (),
			RunningReschedule (),
			Scheduled ();
		}
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class ActorAction
			extends Action<Actor<?>>
	{
		ActorAction (final Actor<?> actor, final Completion completion)
		{
			super (actor.reactor, actor, completion);
		}
	}
	
	static final class ActorAssignAction
			extends ActorAction
	{
		ActorAssignAction (final Actor<?> actor, final CallbackHandler handler, final Scheduler scheduler)
		{
			super (actor, new Completion (actor.reactor));
			this.handler = handler;
			this.scheduler = scheduler;
		}
		
		final CallbackHandler handler;
		final Scheduler scheduler;
	}
	
	static final class ActorCallbackAction
			extends ActorAction
	{
		ActorCallbackAction (final Actor<?> actor, final Method method, final Object[] arguments)
		{
			super (actor, new Completion (actor.reactor));
			this.method = method;
			this.arguments = arguments;
		}
		
		final Object[] arguments;
		final Method method;
	}
	
	static final class ActorDestroyAction
			extends ActorAction
	{
		ActorDestroyAction (final Actor<?> actor, final Completion completion)
		{
			super (actor, completion);
		}
	}
	
	static final class IdentityComparator
			extends Object
			implements
				Comparator<Object>
	{
		IdentityComparator ()
		{
			super ();
		}
		
		@Override
		public final int compare (final Object left, final Object right)
		{
			return ((left.hashCode () - right.hashCode ()));
		}
	}
	
	final static class Reactor
			extends Object
			implements
				ActionTarget
	{
		Reactor (final BasicCallbackReactor facade, final ThreadingContext threading, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (threading);
			Preconditions.checkNotNull (exceptions);
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.facade = facade;
				this.reference = this.facade.new Reference ();
				this.threading = threading;
				this.transcript = Transcript.create (this.facade);
				this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				this.executor = this.threading.createCachedThreadPool (ThreadConfiguration.create (this.facade, "isolates", true));
				this.schedulers = new ConcurrentHashMap<CallbackIsolate, BasicCallbackReactor.Scheduler> ();
				this.actors = new ConcurrentHashMap<CallbackProxy, BasicCallbackReactor.Actor<?>> ();
				this.status = Atomics.newReference (Status.Active);
				this.transcript.traceDebugging ("created reactor `%{object:identity}`.", this.facade);
			}
		}
		
		final boolean await (final long timeout)
		{
			return (Threading.join (this.executor, timeout));
		}
		
		final CallbackIsolate createIsolate ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Scheduler scheduler = new Scheduler (this);
				return (scheduler.isolate);
			}
		}
		
		final <_Callbacks_ extends Callbacks> _Callbacks_ createProxy (final Class<_Callbacks_> specification)
		{
			Preconditions.checkNotNull (specification);
			Preconditions.checkArgument (Callbacks.class.isAssignableFrom (specification));
			Preconditions.checkArgument (!CallbackProxy.class.isAssignableFrom (specification));
			Preconditions.checkArgument (!CallbackHandler.class.isAssignableFrom (specification));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<_Callbacks_> actor = new Actor<_Callbacks_> (this, specification);
				return (specification.cast (actor.proxy));
			}
		}
		
		final void enqueueRunnable (final Runnable runnable)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.transcript.traceDebugging ("enqueueing runnable `%{object}` on reactor `%{object:identity}`...", runnable, this.facade);
				this.executor.execute (runnable);
			}
		}
		
		final void executeDestroy ()
		{
			this.transcript.traceDebugging ("destroying reactor `%{object:identity}`...", this.facade);
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Destroying);
			}
			if (!this.actors.isEmpty ()) {
				for (final Actor<?> actor : this.actors.values ())
					actor.triggerDestroy ();
				this.transcript.traceDebugging ("defer destroying reactor `%{object:identity}` due to registered actors...", this.facade);
				this.enqueueRunnable (new ReactorDestroyAction (this));
				return;
			}
			if (!this.schedulers.isEmpty ()) {
				for (final Scheduler scheduler : this.schedulers.values ())
					scheduler.triggerDestroy ();
				this.transcript.traceDebugging ("defer destroying reactor `%{object:identity}` due to registered schedulers...", this.facade);
				this.enqueueRunnable (new ReactorDestroyAction (this));
				return;
			}
			synchronized (this.monitor) {
				Preconditions.checkState (this.schedulers.isEmpty ());
				Preconditions.checkState (this.actors.isEmpty ());
				this.executor.shutdown ();
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
			}
			this.transcript.traceDebugging ("destroyed reactor `%{object:identity}`.", this.facade);
		}
		
		final void registerActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("registering actor `%{object:identity}` on reactor `%{object:identity}`...", actor, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actors.put (actor.proxy, actor) == null);
			}
		}
		
		final void registerScheduler (final Scheduler scheduler)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("registering scheduler `%{object:identity}` on reactor `%{object:identity}`...", scheduler, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.schedulers.put (scheduler.isolate, scheduler) == null);
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference triggerAssign (final _Callbacks_ proxy, final _Callbacks_ delegate)
		{
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (delegate);
			Preconditions.checkArgument (Callbacks.class.isInstance (delegate));
			Preconditions.checkArgument (CallbackProxy.class.isInstance (delegate));
			Preconditions.checkArgument (!CallbackHandler.class.isInstance (delegate));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerAssign (delegate));
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference triggerAssign (final _Callbacks_ proxy, final CallbackHandler handler, final CallbackIsolate isolate)
		{
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (isolate);
			Preconditions.checkNotNull (handler);
			Preconditions.checkArgument (Callbacks.class.isInstance (handler));
			Preconditions.checkArgument (!CallbackProxy.class.isInstance (handler));
			Preconditions.checkArgument (CallbackHandler.class.isInstance (handler));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				final Scheduler scheduler = this.schedulers.get (isolate);
				Preconditions.checkNotNull (scheduler);
				return (actor.triggerAssign (handler, scheduler));
			}
		}
		
		final void triggerDestroy ()
		{
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return;
						default:
							throw (new IllegalStateException ());
					}
				}
				this.enqueueRunnable (new ReactorDestroyAction (this));
			}
		}
		
		final CallbackReference triggerDestroyIsolate (final CallbackIsolate isolate)
		{
			Preconditions.checkNotNull (isolate);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Scheduler scheduler = this.schedulers.get (isolate);
				Preconditions.checkNotNull (scheduler);
				return (scheduler.triggerDestroy ());
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference triggerDestroyProxy (final _Callbacks_ proxy)
		{
			Preconditions.checkNotNull (proxy);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerDestroy ());
			}
		}
		
		final void unregisterActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("unregistering actor `%{object:identity}` from reactor `%{object:identity}`...", actor, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actors.remove (actor.proxy) == actor);
			}
		}
		
		final void unregisterScheduler (final Scheduler scheduler)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("unregistering scheduler `%{object:identity}` from reactor `%{object:identity}`...", scheduler, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.schedulers.remove (scheduler.isolate) == scheduler);
			}
		}
		
		final ConcurrentHashMap<CallbackProxy, Actor<?>> actors;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final BasicCallbackReactor facade;
		final Monitor monitor;
		final Reference reference;
		final ConcurrentHashMap<CallbackIsolate, Scheduler> schedulers;
		final AtomicReference<Status> status;
		final ThreadingContext threading;
		final Transcript transcript;
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class ReactorAction
			extends Action<Reactor>
	{
		ReactorAction (final Reactor reactor, final Completion completion)
		{
			super (reactor, reactor, completion);
		}
	}
	
	static final class ReactorDestroyAction
			extends ReactorAction
			implements
				Runnable
	{
		ReactorDestroyAction (final Reactor reactor)
		{
			super (reactor, null);
		}
		
		@Override
		public final void run ()
		{
			try {
				this.reactor.executeDestroy ();
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceIgnoredException (exception);
			}
		}
	}
	
	final class Reference
			extends WeakReference<BasicCallbackReactor>
	{
		Reference ()
		{
			super (BasicCallbackReactor.this);
		}
		
		@Override
		public final void clear ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final boolean enqueue ()
		{
			return (super.enqueue ());
		}
		
		@Override
		public BasicCallbackReactor get ()
		{
			return (super.get ());
		}
		
		@Override
		public final boolean isEnqueued ()
		{
			return (super.isEnqueued ());
		}
	}
	
	static final class Scheduler
			extends Object
			implements
				ActionTarget
	{
		Scheduler (final Reactor reactor)
		{
			super ();
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.reactor = reactor;
				this.destroyCompletion = new Completion (this.reactor);
				this.isolate = CallbackIsolate.create (this.reactor.reference, this.destroyCompletion);
				this.actorsRegistered = new ConcurrentSkipListSet<BasicCallbackReactor.Actor<?>> (new IdentityComparator ());
				this.actorsEnqueued = new ConcurrentLinkedQueue<BasicCallbackReactor.Actor<?>> ();
				this.destroyAction = Atomics.newReference (null);
				this.scheduleStatus = Atomics.newReference (ScheduleStatus.Idle);
				this.status = Atomics.newReference (Status.Active);
				this.reactor.registerScheduler (this);
				this.reactor.transcript.traceDebugging ("created isolate `%{object:identity}` (owned by scheduler `%{object:identity}` from reactor `%{object:identity}`).", this.isolate, this, this.reactor.facade);
			}
		}
		
		final void enqueueActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("enqueueing actor `%{object:identity}` on scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.actorsEnqueued.add (actor);
				this.schedule ();
			}
		}
		
		final void executeActions ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Scheduled, ScheduleStatus.Running));
			}
			{
				this.reactor.transcript.traceDebugging ("executing enqueued actors on scheduler `%{object:identity}`...", this);
				while (true) {
					final Actor<?> actor = this.actorsEnqueued.poll ();
					if (actor == null)
						break;
					actor.executeActions ();
				}
			}
			{
				this.reactor.transcript.traceDebugging ("executed enqueued actors on scheduler `%{object:identity}.`", this);
				if (this.destroyAction.get () != null)
					this.executeDestroy ();
			}
			synchronized (this.monitor) {
				if (this.scheduleStatus.get () == ScheduleStatus.RunningReschedule) {
					Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.RunningReschedule, ScheduleStatus.Scheduled));
					this.reactor.enqueueRunnable (new SchedulerExecuteAction (this));
					this.reactor.transcript.traceDebugging ("scheduled scheduler `%{object:identity}`.", this);
				} else
					Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.Idle));
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("destroying scheduler `%{object:identity}`...", this);
				Preconditions.checkState (this.status.get () == Status.Destroying);
				final SchedulerDestroyAction action = this.destroyAction.get ();
				Preconditions.checkNotNull (action);
				if (!this.actorsRegistered.isEmpty ()) {
					for (final Actor<?> actor : this.actorsRegistered)
						actor.triggerDestroy ();
					this.reactor.transcript.traceDebugging ("defer destroying scheduler `%{object:identity}` due to registered actors...", this);
					this.schedule ();
					return;
				}
				Preconditions.checkState (this.actorsRegistered.isEmpty ());
				Preconditions.checkState (this.actorsEnqueued.isEmpty ());
				Preconditions.checkState (this.destroyAction.compareAndSet (action, null));
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
				this.reactor.unregisterScheduler (this);
				action.completion.triggerSuccess ();
				this.reactor.transcript.traceDebugging ("destroyed isolate `%{object:identity}` (owned by scheduler `%{object:identity}).`", this.isolate, this);
			}
		}
		
		final void registerActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("registering actor `%{object:identity}` on scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.actorsRegistered.add (actor));
			}
		}
		
		final void schedule ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				switch (this.scheduleStatus.get ()) {
					case Idle :
						Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Idle, ScheduleStatus.Scheduled));
						this.reactor.enqueueRunnable (new SchedulerExecuteAction (this));
						this.reactor.transcript.traceDebugging ("scheduled scheduler `%{object:identity}`.", this);
						break;
					case Scheduled :
						break;
					case Running :
						Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.RunningReschedule));
						break;
					case RunningReschedule :
						break;
					default:
						throw (new IllegalStateException ());
				}
			}
		}
		
		final CallbackReference triggerDestroy ()
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("destroying (triggered) for scheduler `%{object:identity}`...", this);
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return (this.destroyCompletion.reference);
						default:
							throw (new IllegalStateException ());
					}
				}
				final SchedulerDestroyAction action = new SchedulerDestroyAction (this, this.destroyCompletion);
				Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
				this.schedule ();
				return (action.completion.reference);
			}
		}
		
		final void unregisterActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("unregistering actor `%{object:identity}` from scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actorsRegistered.remove (actor));
			}
		}
		
		final ConcurrentLinkedQueue<Actor<?>> actorsEnqueued;
		final ConcurrentSkipListSet<Actor<?>> actorsRegistered;
		final AtomicReference<SchedulerDestroyAction> destroyAction;
		final Completion destroyCompletion;
		final CallbackIsolate isolate;
		final Monitor monitor;
		final Reactor reactor;
		final AtomicReference<ScheduleStatus> scheduleStatus;
		final AtomicReference<Status> status;
		
		static enum ScheduleStatus
		{
			Idle (),
			Running (),
			RunningReschedule (),
			Scheduled ();
		}
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class SchedulerAction
			extends Action<Scheduler>
	{
		SchedulerAction (final Scheduler scheduler, final Completion completion)
		{
			super (scheduler.reactor, scheduler, completion);
		}
	}
	
	static final class SchedulerDestroyAction
			extends SchedulerAction
	{
		SchedulerDestroyAction (final Scheduler scheduler, final Completion completion)
		{
			super (scheduler, completion);
		}
	}
	
	static final class SchedulerExecuteAction
			extends SchedulerAction
			implements
				Runnable
	{
		SchedulerExecuteAction (final Scheduler scheduler)
		{
			super (scheduler, null);
		}
		
		@Override
		public final void run ()
		{
			try {
				this.target.executeActions ();
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceIgnoredException (exception);
			}
		}
	}
}
